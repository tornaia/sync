package com.github.tornaia.sync.shared.api;

public class GetFileMetaInfoRequest extends AbstractRequest {

    private String userid;
    private String id;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
