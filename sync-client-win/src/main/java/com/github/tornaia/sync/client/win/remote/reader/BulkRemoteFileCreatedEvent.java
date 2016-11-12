package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.shared.api.RemoteEventType;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;

import java.util.ArrayList;
import java.util.List;

public class BulkRemoteFileCreatedEvent {

    private static final int MAX_NUMBER_OF_EVENTS = 20;
    private static final long PREFERRED_MAX_SIZE = 5 * 1024 * 1024;

    public List<RemoteFileEvent> createEvents = new ArrayList<>();

    public synchronized boolean add(RemoteFileEvent remoteFileEvent) {
        if (RemoteEventType.CREATED != remoteFileEvent.eventType) {
            throw new IllegalArgumentException("Cannot add a non-created event: " + remoteFileEvent);
        }
        if (createEvents.isEmpty()) {
            createEvents.add(remoteFileEvent);
            return true;
        }

        if (createEvents.size() >= MAX_NUMBER_OF_EVENTS) {
            return false;
        }

        long currentSize = createEvents.stream().mapToLong(rfe -> rfe.fileMetaInfo.size).sum();
        if (currentSize + remoteFileEvent.fileMetaInfo.size > PREFERRED_MAX_SIZE) {
            return false;
        }

        createEvents.add(remoteFileEvent);
        return true;
    }
}
