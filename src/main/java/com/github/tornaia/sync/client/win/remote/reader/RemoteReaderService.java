package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.client.win.ClientidService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@ClientEndpoint
public class RemoteReaderService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteReaderService.class);

    @Value("${frosch-sync.userid}")
    private String userid;

    @Autowired
    private SyncWebSocketReConnectService syncWebSocketReConnectService;

    @Autowired
    private RemoteRestQueryService remoteRestQueryService;

    @Autowired
    private ClientidService clientidService;

    private final List<RemoteFileEvent> createdEvents = new ArrayList<>();

    private final List<RemoteFileEvent> modifiedEvents = new ArrayList<>();

    private final List<RemoteFileEvent> deletedEvents = new ArrayList<>();


    private volatile boolean initDone;

    private Session session;

    public boolean isInitDone() {
        return initDone;
    }

    @OnOpen
    public void open(Session session) {
        this.session = session;
        LOG.info("Session opened. SessionId: " + session.getId());
        sendMessage("hello-please-send-me-updates-of-" + userid);
        sendMessage("hello-i-am-" + clientidService.clientid);
    }

    @OnMessage
    public void onMessage(String message) {
        LOG.debug("Received msg: " + message);
        if (Objects.equals("init-done", message)) {
            LOG.info("Init done. Number of remote events to process c/m/d: " + createdEvents.size() + ", " + modifiedEvents.size() + ", " + deletedEvents.size());
            initDone = true;
            return;
        }
        RemoteFileEvent remoteFileEvent = new Gson().fromJson(message, RemoteFileEvent.class);
        addNewEvent(remoteFileEvent);
    }

    private void addNewEvent(RemoteFileEvent remoteFileEvent) {
        synchronized (this) {
            switch (remoteFileEvent.eventType) {
                case CREATED:
                    createdEvents.add(remoteFileEvent);
                    break;
                case MODIFIED:
                    modifiedEvents.add(remoteFileEvent);
                    break;
                case DELETED:
                    deletedEvents.add(remoteFileEvent);
                    break;
                default:
                    throw new IllegalStateException("Unknown message: " + remoteFileEvent);
            }
        }
    }

    public Optional<RemoteFileEvent> getNextCreated() {
        synchronized (this) {
            return createdEvents.isEmpty() ? Optional.empty() : Optional.of(createdEvents.remove(0));
        }
    }

    public Optional<RemoteFileEvent> getNextModified() {
        synchronized (this) {
            return modifiedEvents.isEmpty() ? Optional.empty() : Optional.of(modifiedEvents.remove(0));
        }
    }

    public Optional<RemoteFileEvent> getNextDeleted() {
        synchronized (this) {
            return deletedEvents.isEmpty() ? Optional.empty() : Optional.of(deletedEvents.remove(0));
        }
    }

    public void sendMessage(String message) {
        if (!session.isOpen()) {
            LOG.warn("Message will not be sent because the WebSocket session has been closed: " + message);
            return;
        }
        RemoteEndpoint.Async asyncRemote = session.getAsyncRemote();
        asyncRemote.sendText(message);
        LOG.debug("Message sent: " + message);
    }

    @OnClose
    public void closedConnection(Session session) {
        LOG.info("Session closed. SessionId: " + session.getId());
        syncWebSocketReConnectService.reconnect();
    }

    @OnError
    public void error(Session session, Throwable t) {
        LOG.warn("Error on session. SessionId: " + session.getId(), t);
        syncWebSocketReConnectService.reconnect();
    }

    public byte[] getFile(FileMetaInfo fileMetaInfo) {
        return remoteRestQueryService.getFile(fileMetaInfo);
    }
}