package com.github.tornaia.sync.client.win.engine;

import com.github.tornaia.sync.client.win.local.reader.LocalFileEvent;
import com.github.tornaia.sync.client.win.local.reader.LocalReaderService;
import com.github.tornaia.sync.client.win.local.writer.LocalWriterService;
import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
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

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Value("${client.sync.directory.path:C:\\temp\\client}")
    private String syncDirectoryPath;

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
    public void ContextRefreshedEvent() {
        LOG.info("Context refreshed event happened");
        contextIsRunning = true;
        init();
    }

    @EventListener({ContextClosedEvent.class})
    public void onContextClosedEvent() {
        LOG.info("Context closed event happened");
        contextIsRunning = false;
        thread.interrupt();
    }

    public void init() {
        LOG.info("Engine init started");

        new Thread(() -> {
            while (!remoteReaderService.isInitDone()) {
                LOG.trace("Engine init...");
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    LOG.warn("Init terminated: " + e.getMessage());
                }
            }

            runInBackground();
            LOG.info("Engine init finished");
        }).start();
    }

    private void runInBackground() {
        LOG.info("Starting engine...");
        thread = new Thread(() -> {
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

                Optional<RemoteFileEvent> remoteCreatedEvent = remoteReaderService.getNextCreated();
                if (remoteCreatedEvent.isPresent()) {
                    handle(remoteCreatedEvent.get());
                    continue;
                }

                Optional<LocalFileEvent> localCreatedEvent = localReaderService.getNextCreated();
                if (localCreatedEvent.isPresent()) {
                    handle(localCreatedEvent.get());
                    continue;
                }

                Optional<LocalFileEvent> localModifiedEvent = localReaderService.getNextModified();
                if (localModifiedEvent.isPresent()) {
                    handle(localModifiedEvent.get());
                    continue;
                }

                Optional<LocalFileEvent> localDeletedEvent = localReaderService.getNextDeleted();
                if (localDeletedEvent.isPresent()) {
                    handle(localDeletedEvent.get());
                    continue;
                }

                try {
                    // TODO use some kind of blocking queue instead of this ugly sleep
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    LOG.warn("Run terminated: " + e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.setName(userid + "-" + syncDirectoryPath.substring(syncDirectoryPath.length() - 1) + "-Engine");
        thread.start();
    }

    private void handle(RemoteFileEvent remoteEvent) {
        LOG.info("Starting to process remote event: " + remoteEvent);
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
                        LOG.info("Remote and local fileMetaInfo equals: " + remoteFileMetaInfo.relativePath);
                        return;
                    }

                    byte[] remoteFileContent = remoteReaderService.getFile(remoteFileMetaInfo);
                    boolean succeed = false;
                    if (remoteEvent.eventType == RemoteEventType.CREATED) {
                        succeed = localWriterService.write(remoteFileMetaInfo, remoteFileContent);
                    } else if (remoteEvent.eventType == RemoteEventType.MODIFIED) {
                        succeed = localWriterService.replace(relativePath, remoteFileMetaInfo, remoteFileContent);
                    }
                    if (succeed) {
                        LOG.info("File was written to disk: " + remoteFileMetaInfo);
                    } else {
                        LOG.warn("Failed to write file to disk: " + remoteFileMetaInfo);
                    }
                } else {
                    byte[] remoteFileContent = remoteReaderService.getFile(remoteFileMetaInfo);
                    boolean succeed = localWriterService.write(remoteFileMetaInfo, remoteFileContent);
                    if (succeed) {
                        LOG.info("File was created on disk: " + remoteFileMetaInfo);
                    } else {
                        LOG.warn("Failed to create file to disk: " + remoteFileMetaInfo);
                    }
                }
                break;
            case DELETED:
                remoteKnownState.add(remoteFileMetaInfo);
                if (!localFileExists) {
                    LOG.info("Local file does not exist, nothing to delete: " + relativePath);
                    return;
                }

                Optional<FileMetaInfo> optionalLocalFileMetaInfo = localReaderService.getFileMetaInfo(relativePath);
                if (!optionalLocalFileMetaInfo.isPresent()) {
                    LOG.warn("Cannot compare remote file with local file: " + relativePath);
                    return;
                }

                FileMetaInfo localFileMetaInfo = optionalLocalFileMetaInfo.get();
                if (Objects.equals(remoteFileMetaInfo, localFileMetaInfo)) {
                    LOG.info("Remote and local fileMetaInfo equals: " + remoteFileMetaInfo.relativePath);
                    boolean succeed = localWriterService.delete(relativePath);
                    if (succeed) {
                        LOG.info("File was created on disk: " + remoteFileMetaInfo);
                    } else {
                        LOG.warn("Failed to create file to disk: " + remoteFileMetaInfo);
                    }
                } else {
                    LOG.warn("Remote and local fileMetaInfo differs, wont delete! Remote: " + remoteFileMetaInfo + ", localFileInfo: " + localFileMetaInfo);
                }
                break;
            default:
                LOG.warn("Unhandled message: " + remoteEvent);
        }
    }

    private void handle(LocalFileEvent localEvent) {
        LOG.info("Starting to process local event: " + localEvent);
        String relativePath = localEvent.relativePath;
        switch (localEvent.eventType) {
            case CREATED:
                boolean createSucceed = remoteWriterService.createFile(relativePath);
                if (createSucceed) {
                    LOG.info("File is in sync with server after created event: " + relativePath);
                } else {
                    LOG.warn("File creation cannot synced with server: " + relativePath);
                    localReaderService.reAddEvent(localEvent);
                }
                break;
            case MODIFIED:
                boolean modifySucceed = remoteWriterService.modifyFile(relativePath);
                if (modifySucceed) {
                    LOG.info("File is in sync with server after modified event: " + relativePath);
                } else {
                    LOG.warn("File modification cannot synced with server: " + relativePath);
                    localReaderService.reAddEvent(localEvent);
                }
                break;
            case DELETED:
                boolean deleteSucceed = remoteWriterService.deleteFile(relativePath);
                if (deleteSucceed) {
                    LOG.info("File is in sync with server after deleted event: " + relativePath);
                } else {
                    LOG.warn("File deletion cannot synced with server: " + relativePath);
                    localReaderService.reAddEvent(localEvent);
                }
                break;
            default:
                LOG.warn("Unhandled message: " + localEvent);
        }
    }
}
