package com.github.tornaia.sync.shared.api;

public class UpdateFileRequestBuilder {

    private String userid;
    private Long size;
    private Long creationDateTime;
    private Long modificationDateTime;

    public UpdateFileRequest create() {
        UpdateFileRequest updateFileRequest = new UpdateFileRequest();
        updateFileRequest.setUserid(userid);
        updateFileRequest.setSize(size);
        updateFileRequest.setCreationDateTime(creationDateTime);
        updateFileRequest.setModificationDateTime(modificationDateTime);
        return updateFileRequest;
    }

    public UpdateFileRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public UpdateFileRequestBuilder size(long size) {
        this.size = size;
        return this;
    }

    public UpdateFileRequestBuilder creationDateTime(long creationDateTime) {
        this.creationDateTime = creationDateTime;
        return this;
    }

    public UpdateFileRequestBuilder modificationDateTime(long modificationDateTime) {
        this.modificationDateTime = modificationDateTime;
        return this;
    }
}
