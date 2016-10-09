package com.github.tornaia.sync.shared.api;

public class GetFileRequestBuilder {

    private String userid;

    public GetFileRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public GetFileRequest create() {
        GetFileRequest getFileRequest = new GetFileRequest();
        getFileRequest.setUserid(userid);
        return getFileRequest;
    }
}
