package com.github.tornaia.sync.client.win.engine;

import com.github.tornaia.sync.client.win.local.reader.LocalReaderService;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileCreatedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileDeletedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.bulk.LocalFileModifiedBulkEvents;
import com.github.tornaia.sync.client.win.local.reader.event.single.AbstractLocalFileEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;
import com.github.tornaia.sync.client.win.local.writer.LocalWriterService;
import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
import com.github.tornaia.sync.client.win.remote.reader.BulkRemoteFileCreatedEvent;
import com.github.tornaia.sync.client.win.remote.reader.FileGetResponse;
import com.github.tornaia.sync.client.win.remote.reader.RemoteReaderService;
import com.github.tornaia.sync.client.win.remote.writer.RemoteWriterService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class Engine {

    private static final Logger LOG = LoggerFactory.getLogger(Engine.class);

    @Value("${client.sync.userid}")
    private String userid;

    @Value("${client.sync.directory.path}")
    private String directoryPath;

    @Autowired
    private LocalReaderService localReaderService;

    @Autowired
    private LocalWriterService localWriterService;

    @Autowired
    private RemoteReaderService remoteReaderService;

    @Autowired
    private RemoteWriterService remoteWriterService;

    @Autowired
    private RemoteKnownState remoteKnownState;

    private Thread thread;

    private volatile boolean contextIsRunning;

    @EventListener({ContextRefreshedEvent.class})
    public void onContextRefreshedEvent() {
        LOG.info("Context refreshed event happened");
        contextIsRunning = true;
        init();
    }

    @EventListener({ContextClosedEvent.class})
    public void onContextClosedEvent() {
        LOG.info("Context closed event happened");
        contextIsRunning = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void init() {
        LOG.info("Engine init started");

        new Thread(() -> {
            while (!remoteReaderService.isInitDone()) {
                LOG.trace("Init messages are coming...");
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException e) {
                    LOG.warn("Processing of init messages terminated: " + e.getMessage());
                }
            }
            LOG.info("Init messages are all read");

            runInBackground();
        }).start();
    }

    private void runInBackground() {
        thread = new Thread(() -> {
            LOG.info("Engine is running");
            while (contextIsRunning) {
                Optional<RemoteFileEvent> remoteDeletedEvent = remoteReaderService.getNextDeleted();
                if (remoteDeletedEvent.isPresent()) {
                    handle(remoteDeletedEvent.get());
                    continue;
                }
                Optional<RemoteFileEvent> remoteModifiedEvent = remoteReaderService.getNextModified();
                if (remoteModifiedEvent.isPresent()) {
                    handle(remoteModifiedEvent.get());
                    continue;
                }
                BulkRemoteFileCreatedEvent remoteCreatedEvents = remoteReaderService.getNextCreated();
                if (!remoteCreatedEvents.createEvents.isEmpty()) {
                    remoteCreatedEvents.createEvents.stream()
                            .parallel()
                            .forEach(rfe -> handle(rfe));
                    continue;
                }

                LocalFileDeletedBulkEvents localDeletedEvents = localReaderService.getNextDeleted();
                if (!localDeletedEvents.events.isEmpty()) {
                    localDeletedEvents.events.stream()
                            .parallel()
                            .forEach(lfe -> handle(lfe));
                    continue;
                }

                LocalFileModifiedBulkEvents localModifiedEvents = localReaderService.getNextModified();
                if (!localModifiedEvents.events.isEmpty()) {
                    localModifiedEvents.events.stream()
                            .parallel()
                            .forEach(lfe -> handle(lfe));
                    continue;
                }

                LocalFileCreatedBulkEvents localCreatedEvents = localReaderService.getNextCreated();
                if (!localCreatedEvents.events.isEmpty()) {
                    localCreatedEvents.events.stream()
                            .parallel()
                            .forEach(lfe -> handle(lfe));
                    continue;
                }

                if (remoteReaderService.hasNext() || localReaderService.hasNext()) {
                    continue;
                }

                try {
                    // TODO use some kind of blocking mechanism (combined with the hasNext above instead of this ugly sleep
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    LOG.warn("Run terminated: " + e.getMessage());
                }
            }
            LOG.info("Engine stopped");
        });
        thread.setDaemon(true);
        thread.setName(userid + "-" + directoryPath.substring(directoryPath.length() - 1) + "-Engine");
        thread.start();
    }

    private void handle(RemoteFileEvent remoteEvent) {
        LOG.debug("Starting to process remote event: " + remoteEvent);
        FileMetaInfo remoteFileMetaInfo = remoteEvent.fileMetaInfo;
        String relativePath = remoteFileMetaInfo.relativePath;
        boolean localFileExists = localReaderService.exists(relativePath);

        switch (remoteEvent.eventType) {
            case CREATED:
            case MODIFIED:
                remoteKnownState.add(remoteFileMetaInfo);
                if (localFileExists) {
                    Optional<FileMetaInfo> optionalLocalFileMetaInfo = localReaderService.getFileMetaInfo(relativePath);
                    if (!optionalLocalFileMetaInfo.isPresent()) {
                        LOG.warn("Cannot compare remote file with local file: " + relativePath);
                        return;
                    }
                    if (Objects.equals(remoteFileMetaInfo, optionalLocalFileMetaInfo.get())) {
                        LOG.debug("Remote and local fileMetaInfo equals: " + remoteFileMetaInfo.relativePath);
                        return;
                    }
                }

                downloadFile(remoteEvent);
                break;
            case DELETED:
                remoteKnownState.remove(remoteFileMetaInfo);
                if (!localFileExists) {
                    LOG.debug("Local file does not exist, nothing to delete: " + relativePath);
                    return;
                }

                Optional<FileMetaInfo> optionalLocalFileMetaInfo = localReaderService.getFileMetaInfo(relativePath);
                if (!optionalLocalFileMetaInfo.isPresent()) {
                    LOG.warn("Cannot compare remote file with local file: " + relativePath);
                    return;
                }

                FileMetaInfo localFileMetaInfo = optionalLocalFileMetaInfo.get();
                if (Objects.equals(remoteFileMetaInfo, localFileMetaInfo)) {
                    LOG.debug("Remote and local fileMetaInfo equals: " + remoteFileMetaInfo.relativePath);
                    boolean succeed = localWriterService.delete(relativePath);
                    if (succeed) {
                        LOG.debug("File deleted event successfully finished: " + remoteFileMetaInfo);
                    } else {
                        LOG.warn("File deleted event failed: " + remoteFileMetaInfo);
                    }
                } else {
                    LOG.warn("Remote and local fileMetaInfo differs, wont delete! Remote: " + remoteFileMetaInfo + ", localFileInfo: " + localFileMetaInfo);
                }
                break;
            default:
                LOG.warn("Unhandled message: " + remoteEvent);
        }
    }

    private void handle(AbstractLocalFileEvent localEvent) {
        LOG.debug("Starting to process local event: " + localEvent);
        String relativePath = localEvent.relativePath;
        switch (localEvent.eventType) {
            case CREATED:
                boolean createSucceed = remoteWriterService.createFile(relativePath);
                if (createSucceed) {
                    LOG.debug("File is in sync with the server (created): " + relativePath);
                } else {
                    LOG.warn("File creation cannot synced with server: " + relativePath);
                    localReaderService.reAddEvent((LocalFileCreatedEvent) localEvent);
                }
                break;
            case MODIFIED:
                boolean modifySucceed = remoteWriterService.modifyFile(relativePath);
                if (modifySucceed) {
                    LOG.debug("File is in sync with the server (modified): " + relativePath);
                } else {
                    LOG.warn("File modification cannot synced with server: " + relativePath);
                    localReaderService.reAddEvent((LocalFileModifiedEvent) localEvent);
                }
                break;
            case DELETED:
                boolean deleteSucceed = remoteWriterService.deleteFile(relativePath);
                if (deleteSucceed) {
                    LOG.debug("File is in sync with the server (deleted): " + relativePath);
                } else {
                    LOG.warn("File deletion cannot synced with server: " + relativePath);
                    localReaderService.reAddEvent((LocalFileDeletedEvent) localEvent);
                }
                break;
            default:
                LOG.warn("Unhandled message: " + localEvent);
        }
    }

    private void downloadFile(RemoteFileEvent remoteEvent) {
        FileMetaInfo remoteFileMetaInfo = remoteEvent.fileMetaInfo;
        String relativePath = remoteFileMetaInfo.relativePath;
        FileGetResponse fileGetResponse = remoteReaderService.getFile(remoteFileMetaInfo);
        if (FileGetResponse.Status.OK == fileGetResponse.status) {
            boolean succeed = false;
            if (remoteEvent.eventType == RemoteEventType.CREATED) {
                succeed = localWriterService.write(remoteFileMetaInfo, fileGetResponse.content);
            } else if (remoteEvent.eventType == RemoteEventType.MODIFIED) {
                succeed = localWriterService.replace(relativePath, remoteFileMetaInfo, fileGetResponse.content);
            }
            if (succeed) {
                LOG.debug("Remote event process succeed: " + remoteEvent);
            } else {
                LOG.warn("Remote event process failed: " + remoteEvent);
                remoteReaderService.reAddEvent(remoteEvent);
            }
        }

        boolean notFound = FileGetResponse.Status.NOT_FOUND == fileGetResponse.status;
        if (notFound) {
            LOG.warn("File not found so it does not exist anymore: " + remoteFileMetaInfo.relativePath);
            return;
        }

        boolean transferFailed = FileGetResponse.Status.TRANSFER_FAILED == fileGetResponse.status;
        if (transferFailed) {
            LOG.warn("Transfer failed: " + remoteFileMetaInfo.relativePath);
            remoteReaderService.reAddEvent(remoteEvent);
            return;
        }
    }
}
