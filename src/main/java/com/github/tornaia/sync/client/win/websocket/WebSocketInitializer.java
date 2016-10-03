package com.github.tornaia.sync.client.win.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class WebSocketInitializer {

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

    @Bean
    public WebSocketConnectionManager wsConnectionManager() {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client(), handler(), getWebSocketUri());
        manager.setAutoStartup(true);
        return manager;
    }

    @Bean
    public StandardWebSocketClient client() {
        return new StandardWebSocketClient();
    }

    @Bean
    public EchoWebSocketClient handler() {
        return new EchoWebSocketClient();
    }

    private String getWebSocketUri() {
        return serverSchemeWebSocket + "://" + serverHost + ":" + serverPort + webSocketPath;
    }
}
