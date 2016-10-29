package com.github.tornaia.sync.client.win.local.reader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BulkLocalFileModifiedEvent {

    private static final int MAX_NUMBER_OF_EVENTS = 20;
    private static final long PREFERRED_MAX_SIZE = 1 * 1024 * 1024;

    public List<LocalFileEvent> modifyEvents = new ArrayList<>();

    private long sumSize;

    private Path syncDirectory;

    public BulkLocalFileModifiedEvent(Path syncDirectory) {
        this.syncDirectory = syncDirectory;
    }

    public synchronized boolean add(FileModifiedEvent fileModifiedEvent) {
        if (modifyEvents.isEmpty()) {
            modifyEvents.add(fileModifiedEvent);
            return true;
        }

        if (modifyEvents.size() >= MAX_NUMBER_OF_EVENTS) {
            return false;
        }

        String relativePath = fileModifiedEvent.relativePath;
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
        modifyEvents.add(fileModifiedEvent);
        return true;
    }
}
