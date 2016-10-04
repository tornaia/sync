package com.github.tornaia.sync.client.win.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.net.URI;

@Component
public class EchoWebSocketKeepAliveService {

    private static final Logger LOG = LoggerFactory.getLogger(EchoWebSocketKeepAliveService.class);

    @Value("${server.scheme.web.socket:ws}")
    private String serverSchemeWebSocket;

    @Value("${server.host:127.0.0.1}")
    private String serverHost;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.web.socket.path:/echo}")
    private String webSocketPath;

    @Value("${frosch-sync.user.id:7247234}")
    private String userid;

    @Autowired
    private EchoWebSocketClient echoWebSocketClient;

    @EventListener({ContextRefreshedEvent.class})
    public void contextRefreshedEvent() {
        LOG.info("Context refreshed event happened");
        reconnect();
    }

    public void reconnect() {
        LOG.info("Reconnect webSocket");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        new Thread(() -> {
            while (true) {
                try {
                    LOG.info("Reconnecting webSocket...");
                    container.connectToServer(echoWebSocketClient, URI.create(getWebSocketUri()));
                    LOG.info("Successfully connected to webSocket!");
                    break;
                } catch (Exception e) {
                    LOG.warn("Failed to connect to webSocket!");
                }
            }
        }).start();
    }

    private String getWebSocketUri() {
        return serverSchemeWebSocket + "://" + serverHost + ":" + serverPort + webSocketPath;
    }
}
