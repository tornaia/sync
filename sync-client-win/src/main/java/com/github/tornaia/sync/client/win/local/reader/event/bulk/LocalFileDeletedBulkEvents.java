package com.github.tornaia.sync.client.win.local.reader.event.bulk;

import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;

import java.nio.file.Path;

public class LocalFileDeletedBulkEvents extends LocalFileBulkEvents<LocalFileDeletedEvent> {

    private static final long DELETE_EVENTS_CAN_NEVER_BE_BULK = Long.MAX_VALUE;

    public LocalFileDeletedBulkEvents(Path syncDirectory) {
        super(syncDirectory);
    }

    @Override
    protected long getSize(String relativePath) {
        return DELETE_EVENTS_CAN_NEVER_BE_BULK;
    }
}
