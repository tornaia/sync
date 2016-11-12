package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Objects;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_UNIX;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public abstract class AbstractLocalFileEvent {

    public final LocalFileEventType eventType;

    public final String relativePath;

    public AbstractLocalFileEvent(LocalFileEventType eventType, String relativePath) {
        checkArgument(relativePath);
        this.eventType = eventType;
        this.relativePath = relativePath;
    }

    private static void checkArgument(String relativePath) {
        if (relativePath.startsWith(SEPARATOR_WINDOWS) || relativePath.startsWith(SEPARATOR_UNIX)) {
            throw new IllegalArgumentException("RelativePath of a file must not start with separator char: " + relativePath);
        }
        if (relativePath.endsWith(SEPARATOR_WINDOWS) || relativePath.endsWith(SEPARATOR_UNIX)) {
            throw new IllegalArgumentException("RelativePath of a file must not end with separator char: " + relativePath);
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

        AbstractLocalFileEvent other = (AbstractLocalFileEvent) obj;

        return Objects.equals(this.eventType, other.eventType) && Objects.equals(this.relativePath, other.relativePath);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(getClass().getSimpleName(), ToStringStyle.JSON_STYLE)
                .append(this.getClass().getSimpleName(), "")
                .append("eventType", eventType)
                .append("relativePath", relativePath)
                .toString();
    }
}
