package com.github.tornaia.sync.client.win.websocket;

import com.github.tornaia.sync.client.win.statestorage.SyncStateManager;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;

@Component
@ClientEndpoint
public class SyncWebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketClient.class);

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Autowired
    private SyncWebSocketKeepAliveService syncWebSocketKeepAliveService;

    @Autowired
    private SyncStateManager syncStateManager;

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
        FileMetaInfo fileMetaInfo = new Gson().fromJson(message, FileMetaInfo.class);
        syncStateManager.fetch(fileMetaInfo);
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