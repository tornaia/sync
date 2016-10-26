package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.client.win.ClientidService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.util.*;

import static java.util.Collections.reverseOrder;

@Component
@ClientEndpoint
public class RemoteReaderService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteReaderService.class);

    @Value("${client.sync.userid}")
    private String userid;

    @Autowired
    private SyncWebSocketReConnectService syncWebSocketReConnectService;

    @Autowired
    private RemoteRestQueryService remoteRestQueryService;

    @Autowired
    private ClientidService clientidService;

    @Autowired
    private SerializerUtils serializerUtils;

    private final List<RemoteFileEvent> createdEvents = new ArrayList<>();

    private final List<RemoteFileEvent> modifiedEvents = new ArrayList<>();

    private final TreeSet<RemoteFileEvent> deletedEvents = new TreeSet<>(reverseOrder(new RelativePathLengthComparator()));

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
            LOG.info("Init done. Number of remote events to process c/m/d: " + createdEvents.size() + "/" + modifiedEvents.size() + "/" + deletedEvents.size());
            initDone = true;
            return;
        }

        RemoteFileEvent remoteFileEvent = serializerUtils.toObject(message, RemoteFileEvent.class);
        addNewEvent(remoteFileEvent);
    }

    public boolean hasNext() {
        synchronized (this) {
            return !createdEvents.isEmpty() || !modifiedEvents.isEmpty() || !deletedEvents.isEmpty();
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
            if (deletedEvents.isEmpty()) {
                return Optional.empty();
            } else {
                RemoteFileEvent next = deletedEvents.iterator().next();
                deletedEvents.remove(next);
                return Optional.of(next);
            }
        }
    }

    public void sendMessage(String message) {
        if (session == null) {
            LOG.warn("Message will not be sent because the WebSocket session has not been initialized yet: " + message);
            return;
        }

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

    public FileGetResponse getFile(FileMetaInfo fileMetaInfo) {
        FileGetResponse fileGetResponse = remoteRestQueryService.getFile(fileMetaInfo);
        LOG.info("File read from server: " + fileGetResponse.fileMetaInfo + " -> " + fileGetResponse.status);
        return fileGetResponse;
    }

    private void addNewEvent(RemoteFileEvent remoteFileEvent) {
        synchronized (this) {
            switch (remoteFileEvent.eventType) {
                case CREATED:
                    createdEvents.add(0, remoteFileEvent);
                    break;
                case MODIFIED:
                    modifiedEvents.add(0, remoteFileEvent);
                    break;
                case DELETED:
                    deletedEvents.add(remoteFileEvent);
                    break;
                default:
                    throw new IllegalStateException("Unknown message: " + remoteFileEvent);
            }
        }
    }

    private class RelativePathLengthComparator implements Comparator<RemoteFileEvent> {
        public int compare(RemoteFileEvent obj1, RemoteFileEvent obj2) {
            if (obj1 == obj2) {
                return 0;
            }
            if (obj1 == null) {
                return -1;
            }
            if (obj2 == null) {
                return 1;
            }
            return Integer.compare(obj1.fileMetaInfo.relativePath.length(), obj2.fileMetaInfo.relativePath.length());
        }
    }
}