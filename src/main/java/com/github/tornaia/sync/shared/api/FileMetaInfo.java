package com.github.tornaia.sync.shared.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

public class FileMetaInfo implements Serializable {

    public final String id;
    public final String relativePath;
    public final long length;
    public final long creationDateTime;
    public final long modificationDateTime;

    public FileMetaInfo() {
        this.id = null;
        this.relativePath = null;
        this.length = -1;
        this.creationDateTime = -1;
        this.modificationDateTime = -1;
    }

    public static FileMetaInfo createNonSyncedFileMetaInfo(String relativePath, File file) {
        return new FileMetaInfo(null, relativePath, file);
    }

    public FileMetaInfo(String id, String relativePath, File file) {
        this.id = id;
        this.relativePath = relativePath;
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create FileMetaInfo", e);
        }
        this.length = attr.size();
        this.creationDateTime = attr.creationTime().toMillis();
        this.modificationDateTime = attr.lastModifiedTime().toMillis();
    }

    public FileMetaInfo(String id, String relativePath, long length, long creationDateTime, long modificationDateTime) {
        this.id = id;
        this.relativePath = relativePath;
        this.length = length;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(relativePath)
                .append(length)
                .append(creationDateTime)
                .append(modificationDateTime)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileMetaInfo)) {
            return false;
        }

        FileMetaInfo other = (FileMetaInfo) obj;

        return new EqualsBuilder()
                .append(relativePath, other.relativePath)
                .append(length, other.length)
                .append(creationDateTime, other.creationDateTime)
                .append(modificationDateTime, other.modificationDateTime)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder("FileMetaInfo", ToStringStyle.JSON_STYLE)
                .append("FileMetaInfo", "")
                .append("id", id)
                .append("relativePath", relativePath)
                .append("length", length)
                .append("creationDateTime", creationDateTime)
                .append("modificationDateTime", modificationDateTime)
                .toString();
    }
}
