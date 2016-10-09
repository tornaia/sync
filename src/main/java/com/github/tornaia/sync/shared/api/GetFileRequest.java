package com.github.tornaia.sync.shared.api;

public class GetFileRequest extends AbstractRequest {

    private String userid;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
