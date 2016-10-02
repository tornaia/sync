package com.github.tornaia.sync.client.win.websocket;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class WebSocketClientEndpoint {

    private Session userSession;
    private MessageHandler messageHandler;

    public WebSocketClientEndpoint(URI endpointURI) {
        try {
            WebSocketContainer connection = ContainerProvider.getWebSocketContainer();
            System.out.println("endpointURI: " + endpointURI);
            connection.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening webSocket: " + userSession);
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing webSocket");
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    public interface MessageHandler {

        void handleMessage(String message);
    }
}
