package com.github.tornaia.sync.client.win.local.reader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BulkLocalFileCreatedEvent {

    private static final int MAX_NUMBER_OF_EVENTS = 20;
    private static final long PREFERRED_MAX_SIZE = 1 * 1024 * 1024;

    public List<LocalFileEvent> createEvents = new ArrayList<>();

    private long sumSize;

    private Path syncDirectory;

    public BulkLocalFileCreatedEvent(Path syncDirectory) {
        this.syncDirectory = syncDirectory;
    }

    public synchronized boolean add(FileCreatedEvent fileCreatedEvent) {
        if (createEvents.isEmpty()) {
            createEvents.add(fileCreatedEvent);
            return true;
        }

        if (createEvents.size() >= MAX_NUMBER_OF_EVENTS) {
            return false;
        }

        String relativePath = fileCreatedEvent.relativePath;
        Path resolve = syncDirectory.resolve(relativePath);

        File file = resolve.toFile();
        boolean exist = file.exists();
        if (!exist) {
            return false;
        }

        long size = file.length();
        if (sumSize + size > PREFERRED_MAX_SIZE) {
            return false;
        }

        sumSize += size;
        createEvents.add(fileCreatedEvent);
        return true;
    }
}
