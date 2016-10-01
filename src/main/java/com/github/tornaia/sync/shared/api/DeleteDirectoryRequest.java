package com.github.tornaia.sync.shared.api;

public class DeleteDirectoryRequest {

    private String userid;
    private String relativePath;

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
}
