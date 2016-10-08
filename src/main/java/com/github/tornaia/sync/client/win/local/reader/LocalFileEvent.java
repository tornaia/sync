package com.github.tornaia.sync.client.win.local.reader;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class LocalFileEvent {

    public final LocalEventType eventType;

    public final String relativePath;

    public LocalFileEvent(LocalEventType eventType, String relativePath) {
        this.eventType = eventType;
        this.relativePath = relativePath;
    }

    @Override
    public String toString() {
        return new ToStringBuilder("LocalFileEvent", ToStringStyle.JSON_STYLE)
                .append("LocalFileEvent", "")
                .append("eventType", eventType)
                .append("relativePath", relativePath)
                .toString();
    }
}
