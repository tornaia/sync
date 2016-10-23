package com.github.tornaia.sync.server.websocket;

import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SyncWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketHandler.class);

    private final Map<String, List<WebSocketSession>> useridAndSessions = new HashMap<>();

    private final Map<String, WebSocketSession> clientidAndSession = new HashMap<>();

    @Autowired
    private FileQueryService fileQueryService;

    @Autowired
    private SerializerUtils serializerUtils;

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("Connection established. SessionId: " + session.getId());
    }

    @Override
    public synchronized void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String messageStr = message.getPayload();
        if (Objects.equals("ping", messageStr)) {
            LOG.debug("Message received: " + messageStr);
        } else if (messageStr.startsWith("hello-i-am-")) {
            LOG.info("Message received: " + messageStr);
            String clientid = messageStr.substring("hello-i-am-".length());
            clientidAndSession.put(clientid, session);
        } else if (messageStr.startsWith("hello-please-send-me-updates-of-")) {
            LOG.info("Message received: " + messageStr);
            String userid = messageStr.substring("hello-please-send-me-updates-of-".length());
            if (!useridAndSessions.containsKey(userid)) {
                useridAndSessions.put(userid, new ArrayList<>());
            }
            useridAndSessions.get(userid).add(session);
            LOG.info("Session " + session.getId() + " subscribed for all events of user " + userid);
            sendCompleteStatus(session, userid);
        } else {
            LOG.warn("Unknown message: " + messageStr);
        }
    }

    private void sendCompleteStatus(WebSocketSession session, String userid) throws IOException {
        List<FileMetaInfo> modifiedFiles = fileQueryService.getModifiedFiles(userid, Long.MIN_VALUE);

        List<RemoteFileEvent> initMessages = modifiedFiles.stream()
                .map(mf -> new RemoteFileEvent(RemoteEventType.CREATED, new FileMetaInfo(mf.id, mf.userid, mf.relativePath, mf.size, mf.creationDateTime, mf.modificationDateTime)))
                .collect(Collectors.toList());

        initMessages.forEach(rfe -> sendMsg(session, rfe));

        synchronized (session) {
            session.sendMessage(new TextMessage("init-done"));
        }
    }

    @Override
    public synchronized void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOG.info("Transport error. Exception: " + exception.getMessage() + ". SessionId: " + session.getId());
        super.handleTransportError(session, exception);
    }

    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOG.info("Connection closed. Reason: " + status.getReason() + ". SessionId: " + session.getId());
        super.afterConnectionClosed(session, status);

        Optional<List<WebSocketSession>> webSessions = useridAndSessions.values()
                .stream()
                .filter(list -> list.contains(session))
                .findFirst();

        if (webSessions.isPresent()) {
            List<WebSocketSession> webSocketSessions = webSessions.get();
            webSocketSessions.remove(session);
        } else {
            LOG.warn("WebSession was not found: " + session);
        }

        Optional<Map.Entry<String, WebSocketSession>> clientidAndWebSession = clientidAndSession.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), session))
                .findFirst();

        if (clientidAndWebSession.isPresent()) {
            String clientid = clientidAndWebSession.get().getKey();
            clientidAndSession.remove(clientid);
        } else {
            LOG.warn("Clientid not found for session: " + session);
        }
    }

    public void notifyClientsExceptForSource(String sourceClientid, RemoteFileEvent remoteFileEvent) {
        List<WebSocketSession> webSocketSessionsToNotify = useridAndSessions.get(remoteFileEvent.fileMetaInfo.userid);
        if (webSocketSessionsToNotify == null) {
            return;
        }

        WebSocketSession sourceSession = clientidAndSession.get(sourceClientid);
        if (sourceSession == null) {
            LOG.warn("source normally should have a webSocket session too: " + sourceClientid);
        }

        webSocketSessionsToNotify.stream()
                .filter(session -> !Objects.equals(session, sourceSession))
                .forEach(session -> sendMsg(session, remoteFileEvent));
    }

    private void sendMsg(WebSocketSession session, RemoteFileEvent remoteFileEvent) {
        try {
            LOG.debug("Notifying session " + session.getId() + " about a new event: " + remoteFileEvent);
            String remoteFileEventAsJson = serializerUtils.toJSON(remoteFileEvent);
            synchronized (session) {
                session.sendMessage(new TextMessage(remoteFileEventAsJson));
            }
        } catch (SocketTimeoutException e) {
            LOG.debug("Client timeout");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}