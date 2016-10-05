package com.github.tornaia.sync.server.data.document;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.file.Paths;

@Document
public class File {

    @Id
    private String id;

    private String userid;

    private String path;

    private byte[] data;

    private long creationDate;

    private long lastModifiedDate;

    public File() {
    }

    public File(String userid, String path, byte[] data, long creationDate, long lastModifiedDate) {
        this.userid = userid;
        this.path = path;
        this.data = data;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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

    public String getName() {
        if (path == null || Paths.get(getPath()).getFileName() == null) {
            return StringUtils.EMPTY;
        }
        return Paths.get(getPath()).getFileName().toString();
    }

}
