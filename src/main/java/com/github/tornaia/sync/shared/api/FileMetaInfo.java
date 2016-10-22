package com.github.tornaia.sync.shared.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DIRECTORY_POSTFIX;

public class FileMetaInfo implements Serializable {

    public final String id;
    public final String userid;
    public final String relativePath;
    public final long size;
    public final long creationDateTime;
    public final long modificationDateTime;

    public FileMetaInfo() {
        this.id = null;
        this.userid = null;
        this.relativePath = null;
        this.size = -1;
        this.creationDateTime = -1;
        this.modificationDateTime = -1;
    }

    public static FileMetaInfo createNonSyncedFileMetaInfo(String userid, String relativePath, File file) throws IOException {
        return new FileMetaInfo(null, userid, relativePath, file);
    }

    public FileMetaInfo(String id, String userid, String relativePath, File file) throws IOException {
        this.id = id;
        this.userid = userid;
        this.relativePath = relativePath;
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        this.size = isFile() ? attr.size() : 0L;
        this.creationDateTime = attr.creationTime().toMillis();
        this.modificationDateTime = attr.lastModifiedTime().toMillis();
    }

    public FileMetaInfo(String id, String userid, String relativePath, long size, long creationDateTime, long modificationDateTime) {
        this.id = id;
        this.userid = userid;
        this.relativePath = relativePath;
        this.size = size;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }

    @JsonIgnore
    public boolean isFile() {
        return !relativePath.endsWith(DIRECTORY_POSTFIX);
    }

    @JsonIgnore
    public boolean isDirectory() {
        return !isFile();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(userid)
                .append(relativePath)
                .append(size)
                .append(isFile() ? creationDateTime : 0L)
                .append(isFile() ? modificationDateTime : 0L)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileMetaInfo)) {
            return false;
        }

        FileMetaInfo other = (FileMetaInfo) obj;

        return new EqualsBuilder()
                .append(userid, other.userid)
                .append(relativePath, other.relativePath)
                .append(size, other.size)
                .append(isFile() ? creationDateTime : 0L, other.isFile() ? other.creationDateTime : 0L)
                .append(isFile() ? modificationDateTime : 0L, other.isFile() ? other.modificationDateTime : 0L)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder("FileMetaInfo", ToStringStyle.JSON_STYLE)
                .append("FileMetaInfo", "")
                .append("id", id)
                .append("userid", userid)
                .append("relativePath", relativePath)
                .append("size", size)
                .append("creationDateTime", creationDateTime)
                .append("modificationDateTime", modificationDateTime)
                .toString();
    }
}
