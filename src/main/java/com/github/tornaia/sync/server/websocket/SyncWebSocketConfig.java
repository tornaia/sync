package com.github.tornaia.sync.server.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
public class SyncWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(echoWebSocketHandler(), "/echo").setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler echoWebSocketHandler() {
        return new EchoWebSocketHandler();
    }

    public class EchoWebSocketHandler extends TextWebSocketHandler {
        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String messageStr = message.getPayload();
            System.out.println("Message received: " + messageStr);
            String echoResponse = "Echo! " + messageStr;
            session.sendMessage(new TextMessage(echoResponse));
            System.out.println("Message sent: " + echoResponse);
        }
    }
}
