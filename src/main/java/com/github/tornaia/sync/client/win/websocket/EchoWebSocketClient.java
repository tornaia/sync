package com.github.tornaia.sync.client.win.websocket;

import com.github.tornaia.sync.client.win.statestorage.SyncStateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;

@Component
@ClientEndpoint
public class EchoWebSocketClient {

    @Autowired
    private EchoWebSocketKeepAliveService echoWebSocketKeepAliveService;

    private Session session;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        System.out.println("New session opened: " + session);
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received msg: " + message);
    }

    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
        System.out.println("Sent msg: " + message);
    }

    @OnClose
    public void closedConnection(Session session) {
        System.out.println("session closed: " + session);
        echoWebSocketKeepAliveService.reconnect();
    }

    @OnError
    public void error(Session session, Throwable t) {
        System.err.println("Error on session " + session);
        echoWebSocketKeepAliveService.reconnect();
    }
}