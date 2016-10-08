package com.github.tornaia.sync.shared.api;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RemoteFileEvent {

    public final RemoteEventType eventType;

    public final FileMetaInfo fileMetaInfo;

    public RemoteFileEvent(RemoteEventType eventType, FileMetaInfo fileMetaInfo) {
        this.eventType = eventType;
        this.fileMetaInfo = fileMetaInfo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder("RemoteFileEvent", ToStringStyle.JSON_STYLE)
                .append("RemoteFileEvent", "")
                .append("eventType", eventType)
                .append("fileMetaInfo", fileMetaInfo)
                .toString();
    }
}
