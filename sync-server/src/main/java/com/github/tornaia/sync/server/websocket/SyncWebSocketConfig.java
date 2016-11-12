package com.github.tornaia.sync.server.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
public class SyncWebSocketConfig implements WebSocketConfigurer {

    @Value("${sync.pusher.server.path}")
    private String webSocketPath;

    @Autowired
    private SyncWebSocketHandler syncWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(syncWebSocketHandler, webSocketPath).setAllowedOrigins("*");
    }
}
