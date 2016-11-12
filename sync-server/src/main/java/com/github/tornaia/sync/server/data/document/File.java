package com.github.tornaia.sync.server.data.document;

import com.github.tornaia.sync.shared.constant.FileSystemConstants;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.file.Paths;

@Document
public class File {

    @Id
    private String id;

    @Indexed
    private String userid;

    @Indexed
    private String relativePath;

    private long size;

    private long creationDate;

    private long lastModifiedDate;

    public File() {
    }

    public File(String userid, String relativePath, long size, long creationDate, long lastModifiedDate) {
        this.userid = userid;
        this.relativePath = relativePath;
        this.size = size;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    public boolean isFile() {
        return !isDirectory();
    }

    public boolean isDirectory() {
        return relativePath.endsWith(FileSystemConstants.DIRECTORY_POSTFIX);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder("File", ToStringStyle.JSON_STYLE)
                .append("File", "")
                .append("id", id)
                .append("userid", userid)
                .append("relativePath", relativePath)
                .append("size", size)
                .append("creationDate", creationDate)
                .append("lastModifiedDate", lastModifiedDate)
                .toString();
    }
}
