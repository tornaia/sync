package com.github.tornaia.sync.client.win.local.reader.event.bulk;

import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;

import java.nio.file.Path;

public class LocalFileCreatedBulkEvents extends LocalFileBulkEvents<LocalFileCreatedEvent> {

    public LocalFileCreatedBulkEvents(Path syncDirectory) {
        super(syncDirectory);
    }
}
