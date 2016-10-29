package com.github.tornaia.sync.client.win.local.reader.event.bulk;

import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;

import java.nio.file.Path;

public class LocalFileModifiedBulkEvents extends LocalFileBulkEvents<LocalFileModifiedEvent> {

    public LocalFileModifiedBulkEvents(Path syncDirectory) {
        super(syncDirectory);
    }
}
