package com.github.tornaia.sync.client.win.websocket;

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

    @Value("#{systemProperties['server.scheme.web.socket'] ?: 'ws'}")
    private String serverSchemeWebSocket;

    @Value("#{systemProperties['server.host'] ?: '127.0.0.1'}")
    private String serverHost;

    @Value("#{systemProperties['server.port'] ?: '8080'}")
    private int serverPort;

    @Value("#{systemProperties['server.web.socket.path'] ?: '/echo'}")
    private String webSocketPath;

    @Value("#{systemProperties['frosch-sync.user.id'] ?: '7247234'}")
    private String userid;

    @Autowired
    private EchoWebSocketClient echoWebSocketClient;

    @EventListener({ContextRefreshedEvent.class})
    public void contextRefreshedEvent() {
        System.out.println("Context refreshed event happened");
        reconnect();
    }

    public void reconnect() {
        System.out.println("Reconnect webSocket");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        new Thread(() -> {
            while (true) {
                try {
                    container.connectToServer(echoWebSocketClient, URI.create(getWebSocketUri()));
                    System.out.println("Successfully connected to webSocket!");
                    break;
                } catch (Exception e) {
                    System.out.println("Failed to connect to webSocket!");
                }
            }
        }).start();
    }

    private String getWebSocketUri() {
        return serverSchemeWebSocket + "://" + serverHost + ":" + serverPort + webSocketPath;
    }
}
