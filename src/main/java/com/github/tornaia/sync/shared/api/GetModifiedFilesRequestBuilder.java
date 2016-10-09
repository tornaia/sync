package com.github.tornaia.sync.shared.api;

public class GetModifiedFilesRequestBuilder {

    private String userid;
    private long modTS;

    public GetModifiedFilesRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public GetModifiedFilesRequestBuilder modTS(long modTS) {
        this.modTS = modTS;
        return this;
    }

    public GetModifiedFilesRequest create() {
        GetModifiedFilesRequest getModifiedFilesRequest = new GetModifiedFilesRequest();
        getModifiedFilesRequest.setUserid(userid);
        getModifiedFilesRequest.setModTs(modTS);
        return getModifiedFilesRequest;
    }
}
