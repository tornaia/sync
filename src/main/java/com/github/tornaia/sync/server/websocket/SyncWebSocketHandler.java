package com.github.tornaia.sync.server.websocket;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tornaia.sync.server.service.FileQueryService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

@Component
public class SyncWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SyncWebSocketHandler.class);

    private final Map<String, List<WebSocketSession>> usersAndSessions = new HashMap<>();

    @Autowired
    private FileQueryService fileQueryService;

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("Connection established. SessionId: " + session.getId());
    }

    @Override
    public synchronized void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String messageStr = message.getPayload();
        LOG.info("Message received: " + messageStr);
        if (messageStr.startsWith("hello-please-send-me-updates-of-")) {
            String userid = messageStr.substring("hello-please-send-me-updates-of-".length());
            if (!usersAndSessions.containsKey(userid)) {
                usersAndSessions.put(userid, new ArrayList<>());
            }
            usersAndSessions.get(userid).add(session);
            LOG.info("Session " + session.getId() + " subscribed for all events of user " + userid);
            sendCompleteStatus(session, userid);
        } else {
            LOG.warn("Unknown message: " + messageStr);
        }
    }

    private void sendCompleteStatus(WebSocketSession session, String userid) throws IOException {
        List<FileMetaInfo> modifiedFiles = fileQueryService.getModifiedFiles(userid, Long.MIN_VALUE);

        modifiedFiles.stream()
                .map(mf -> new RemoteFileEvent(RemoteEventType.CREATED, new FileMetaInfo(mf.id, mf.userid, mf.relativePath, mf.length, mf.creationDateTime, mf.modificationDateTime)))
                .forEach(this::notifyClients);

        session.sendMessage(new TextMessage("init-done"));
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

        usersAndSessions.values()
                .stream()
                .filter(list -> list.contains(session))
                .findFirst()
                .get()
                .remove(session);
    }

    public void notifyClients(RemoteFileEvent remoteFileEvent) {
        List<WebSocketSession> webSocketSessionsToNotify = usersAndSessions.get(remoteFileEvent.fileMetaInfo.userid);
        if (Objects.isNull(webSocketSessionsToNotify)) {
            return;
        }
        webSocketSessionsToNotify.stream()
                .forEach(session -> {
                    try {
                        LOG.info("Notifying session " + session.getId() + " about a new event: " + remoteFileEvent);
                        ObjectMapper mapper = new ObjectMapper();
                        // TODO move object mapper to a common place and write a test String, int -> "xx", 34 but should be "xx", "34" otherwise client will not able to parse it
                        // or maybe works, I dont know at the moment
                        mapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
                        String remoteFileEventAsJson = mapper.writeValueAsString(remoteFileEvent);
                        // TODO  here we send messages in synchronous way I guess... thats baaad.
                        session.sendMessage(new TextMessage(remoteFileEventAsJson));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}