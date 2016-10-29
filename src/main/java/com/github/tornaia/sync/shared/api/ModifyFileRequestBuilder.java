package com.github.tornaia.sync.shared.api;

public class ModifyFileRequestBuilder {

    private String userid;
    private Long size;
    private Long creationDateTime;
    private Long modificationDateTime;

    public ModifyFileRequest create() {
        ModifyFileRequest modifyFileRequest = new ModifyFileRequest();
        modifyFileRequest.setUserid(userid);
        modifyFileRequest.setSize(size);
        modifyFileRequest.setCreationDateTime(creationDateTime);
        modifyFileRequest.setModificationDateTime(modificationDateTime);
        return modifyFileRequest;
    }

    public ModifyFileRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public ModifyFileRequestBuilder size(long size) {
        this.size = size;
        return this;
    }

    public ModifyFileRequestBuilder creationDateTime(long creationDateTime) {
        this.creationDateTime = creationDateTime;
        return this;
    }

    public ModifyFileRequestBuilder modificationDateTime(long modificationDateTime) {
        this.modificationDateTime = modificationDateTime;
        return this;
    }
}
