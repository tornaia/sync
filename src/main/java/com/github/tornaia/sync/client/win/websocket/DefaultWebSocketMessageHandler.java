package com.github.tornaia.sync.client.win.websocket;

public class DefaultWebSocketMessageHandler implements WebSocketMessageHandler {

    @Override
    public void handleMessage(String message) {
        System.out.println("Message in: " + message);
    }
}
