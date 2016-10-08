package com.github.tornaia.sync.client.win.engine;

import com.github.tornaia.sync.client.win.local.reader.LocalFileEvent;
import com.github.tornaia.sync.client.win.local.reader.LocalReaderService;
import com.github.tornaia.sync.client.win.local.writer.LocalWriterService;
import com.github.tornaia.sync.client.win.remote.reader.RemoteReaderService;
import com.github.tornaia.sync.client.win.remote.writer.RemoteWriterService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Optional;

@Component
public class Engine {

    private static final Logger LOG = LoggerFactory.getLogger(Engine.class);

    @Autowired
    private LocalReaderService localReaderService;

    @Autowired
    private LocalWriterService localWriterService;

    @Autowired
    private RemoteReaderService remoteReaderService;

    @Autowired
    private RemoteWriterService remoteWriterService;

    @PostConstruct
    public void init() throws InterruptedException {
        LOG.info("Engine init started");

        while (remoteReaderService.isInitDone()) {
            LOG.info("Engine init...");
            Thread.sleep(1000L);
        }

        runInBackground();
        LOG.info("Engine init finished");
    }

    private void runInBackground() {
        LOG.info("Starting engine...");
        Thread thread = new Thread(() -> {
            while (true) {
                Optional<RemoteFileEvent> remoteEvent = remoteReaderService.getNext();
                if (remoteEvent.isPresent()) {
                    handle(remoteEvent.get());
                    continue;
                }

                Optional<LocalFileEvent> localEvent = localReaderService.getNext();
                if (localEvent.isPresent()) {
                    handle(localEvent.get());
                    continue;
                }

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    LOG.warn("Sleep interrupted", e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void handle(RemoteFileEvent remoteEvent) {
        switch (remoteEvent.eventType) {
            case CREATED:
                FileMetaInfo remoteFileMetaInfo = remoteEvent.fileMetaInfo;
                String relativePath = remoteFileMetaInfo.relativePath;

                boolean localFileExists = localReaderService.exists(relativePath);

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
                    boolean succeed = localWriterService.replace(relativePath, remoteFileMetaInfo, remoteFileContent);
                    if (succeed) {
                        LOG.info("File was replaced on disk: " + remoteFileMetaInfo);
                    } else {
                        LOG.warn("Failed to replace file on disk: " + remoteFileMetaInfo);
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
            default:
                LOG.warn("Unhandled message: " + remoteEvent);
        }
    }

    private void handle(LocalFileEvent localEvent) {
        switch (localEvent.eventType) {
            case CREATED:
                String relativePath = localEvent.relativePath;
                boolean succeed = remoteWriterService.createFile(relativePath);
                if (succeed) {
                    LOG.info("File was sent to server: " + relativePath);
                } else {
                    LOG.warn("Failed to send file to server: " + relativePath);
                }
                break;
            default:
                LOG.warn("Unhandled message: " + localEvent);
        }
    }
}
