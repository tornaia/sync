package com.github.tornaia.sync.client.win.remote;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

public class RemoteFileEvent {

    public final RemoteEventType eventType;

    public final FileMetaInfo fileMetaInfo;

    public RemoteFileEvent(RemoteEventType eventType, FileMetaInfo fileMetaInfo) {
        this.eventType = eventType;
        this.fileMetaInfo = fileMetaInfo;
    }
}
