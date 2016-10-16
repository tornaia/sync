package com.github.tornaia.sync.client.win.local.reader;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Objects;

public abstract class LocalFileEvent {

    public final LocalEventType eventType;

    public final String relativePath;

    public LocalFileEvent(LocalEventType eventType, String relativePath) {
        this.eventType = eventType;
        this.relativePath = relativePath;
    }

    @Override
    public final int hashCode() {
        int result = 1;
        result = 31 * result + ((eventType == null) ? 0 : eventType.ordinal());
        result = 31 * result + ((relativePath == null) ? 0 : relativePath.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        LocalFileEvent other = (LocalFileEvent) obj;

        return Objects.equals(this.eventType, other.eventType) && Objects.equals(this.relativePath, other.relativePath);
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
