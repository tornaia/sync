package com.github.tornaia.sync.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
public class SyncWebSocketConfig implements WebSocketConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketConfig.class);

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
            LOG.info("Message received: " + messageStr);
            String echoResponse = "Echo! " + messageStr;
            session.sendMessage(new TextMessage(echoResponse));
            LOG.info("Message sent: " + echoResponse);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            LOG.info("Transport error. Exception: " + exception.getMessage() + ". SessionId: " + session.getId());
            super.handleTransportError(session, exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            LOG.info("Connection closed. Reason: " + status.getReason() + ". SessionId: " + session.getId());
            super.afterConnectionClosed(session, status);
        }
    }
}
