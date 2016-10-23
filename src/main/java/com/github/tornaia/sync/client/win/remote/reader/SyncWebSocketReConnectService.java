package com.github.tornaia.sync.client.win.remote.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.net.URI;

@Component
public class SyncWebSocketReConnectService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketReConnectService.class);

    @Value("${sync.pusher.server.scheme}")
    private String pusherServerScheme;

    @Value("${sync.pusher.server.host}")
    private String pusherServerHost;

    @Value("${sync.pusher.server.port}")
    private int pusherServerPort;

    @Value("${sync.pusher.server.path}")
    private String pusherServerPath;

    @Value("${client.sync.userid}")
    private String userid;

    @Value("${client.sync.directory.path}")
    private String directoryPath;

    @Autowired
    private RemoteReaderService remoteReaderService;

    private volatile boolean contextIsRunning;

    @EventListener({ContextRefreshedEvent.class})
    public void contextRefreshedEvent() {
        LOG.info("Context refreshed event happened");
        contextIsRunning = true;
        reconnect();
    }

    @EventListener({ContextClosedEvent.class})
    public void onContextClosedEvent() {
        LOG.info("Context closed event happened");
        contextIsRunning = false;
    }

    public void reconnect() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Thread thread = new Thread(() -> {
            while (contextIsRunning) {
                try {
                    LOG.trace("WebSocket connection retry...");
                    container.connectToServer(remoteReaderService, URI.create(getWebSocketUri()));
                    LOG.info("WebSocket successfully connected");
                    break;
                } catch (Exception e) {
                    LOG.warn("WebSocket connection problem: " + e.getMessage());
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ie) {
                        LOG.warn("Run terminated: " + ie.getMessage());
                    }
                }
            }
            LOG.debug("WebSocket re-connect service fades out since the connection is up");
        });
        thread.setDaemon(true);
        thread.setName(userid + "-" + directoryPath.substring(directoryPath.length() - 1) + "-WSKpAl");
        thread.start();
    }

    private String getWebSocketUri() {
        return pusherServerScheme + "://" + pusherServerHost + ":" + pusherServerPort + pusherServerPath;
    }
}
