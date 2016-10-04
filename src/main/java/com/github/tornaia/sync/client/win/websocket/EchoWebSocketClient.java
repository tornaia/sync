package com.github.tornaia.sync.client.win.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;

@Component
@ClientEndpoint
public class EchoWebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(EchoWebSocketClient.class);

    @Autowired
    private EchoWebSocketKeepAliveService echoWebSocketKeepAliveService;

    private Session session;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        LOG.info("Session opened. SessionId: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message) {
        LOG.info("Received msg: " + message);
    }

    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
        LOG.info("Sent msg: " + message);
    }

    @OnClose
    public void closedConnection(Session session) {
        LOG.info("Session closed. SessionId: " + session.getId());
        echoWebSocketKeepAliveService.reconnect();
    }

    @OnError
    public void error(Session session, Throwable t) {
        LOG.info("Error on session. SessionId: " + session.getId());
        echoWebSocketKeepAliveService.reconnect();
    }
}