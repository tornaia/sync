package com.github.tornaia.sync.shared.api;

public class RemoteFileEvent {

    public final RemoteEventType eventType;

    public final FileMetaInfo fileMetaInfo;

    public RemoteFileEvent(RemoteEventType eventType, FileMetaInfo fileMetaInfo) {
        this.eventType = eventType;
        this.fileMetaInfo = fileMetaInfo;
    }
}
