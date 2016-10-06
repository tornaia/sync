package com.github.tornaia.sync.client.win.remote;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.util.ArrayList;
import java.util.List;

@Component
@ClientEndpoint
public class SyncWebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketClient.class);

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Autowired
    private SyncWebSocketKeepAliveService syncWebSocketKeepAliveService;

    private final List<RemoteFileEvent> events = new ArrayList<>();

    private Session session;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        LOG.info("Session opened. SessionId: " + session.getId());
        sendMessage("hello-please-send-me-updates-of-" + userid);
    }

    @OnMessage
    public void onMessage(String message) {
        LOG.info("Received msg: " + message);
        RemoteFileEvent remoteFileEvent = new Gson().fromJson(message, RemoteFileEvent.class);
    }

    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
        LOG.info("Sent msg: " + message);
    }

    @OnClose
    public void closedConnection(Session session) {
        LOG.info("Session closed. SessionId: " + session.getId());
        syncWebSocketKeepAliveService.reconnect();
    }

    @OnError
    public void error(Session session, Throwable t) {
        LOG.info("Error on session. SessionId: " + session.getId(), t);
        syncWebSocketKeepAliveService.reconnect();
    }
}