package com.github.tornaia.sync.client.win.websocket;

import javax.websocket.*;
import java.net.URI;
import java.util.Objects;

@ClientEndpoint
public class WebSocketClientEndpoint {

    private Session userSession;
    private WebSocketMessageHandler messageHandler;

    public WebSocketClientEndpoint(URI endpointURI) {
        System.out.println("EndpointURI: " + endpointURI);
        try {
            WebSocketContainer connection = ContainerProvider.getWebSocketContainer();
            connection.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("Opening webSocket: " + userSession);
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("Closing webSocket");
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Incoming message: " + message);
        if (Objects.isNull(messageHandler)) {
            return;
        }
        this.messageHandler.handleMessage(message);
    }

    public void setMessageHandler(WebSocketMessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        System.out.println("Outgoing message: " + message);
        this.userSession.getAsyncRemote().sendText(message);
    }
}
