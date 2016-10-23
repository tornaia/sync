package com.github.tornaia.sync.client.win.remote.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncWebSocketKeepAliveService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketKeepAliveService.class);

    @Autowired
    private RemoteReaderService remoteReaderService;

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void keepConnectionAliveBySendingPingMessage() {
        try {
            remoteReaderService.sendMessage("ping");
        } catch (IllegalStateException e) {
            LOG.warn("Failed to send ping messages", e);
        }
    }
}
