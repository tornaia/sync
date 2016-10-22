package com.github.tornaia.sync.client.win.local.reader;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Objects;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_UNIX;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public abstract class LocalFileEvent {

    public final LocalEventType eventType;

    public final String relativePath;

    public LocalFileEvent(LocalEventType eventType, String relativePath) {
        checkArgument(relativePath);
        this.eventType = eventType;
        this.relativePath = relativePath;
    }

    private static void checkArgument(String relativePath) {
        if (relativePath.startsWith(SEPARATOR_WINDOWS) || relativePath.startsWith(SEPARATOR_UNIX)) {
            throw new IllegalArgumentException("RelativePath of a file must never start with separator char: " + relativePath);
        }
        if (relativePath.endsWith(SEPARATOR_WINDOWS) || relativePath.endsWith(SEPARATOR_UNIX)) {
            throw new IllegalArgumentException("RelativePath of a file must never end with separator char: " + relativePath);
        }
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
