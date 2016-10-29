package com.github.tornaia.sync.client.win.local.reader.event.bulk;

import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;

import java.nio.file.Path;

public class LocalFileDeletedBulkEvents extends LocalFileBulkEvents<LocalFileDeletedEvent> {

    public LocalFileDeletedBulkEvents(Path syncDirectory) {
        super(syncDirectory);
    }

    @Override
    protected long getSize(String relativePath) {
        return 0L;
    }
}
