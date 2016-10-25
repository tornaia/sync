package com.github.tornaia.sync.shared.api;

public class DeleteFileRequestBuilder {

    private String id;
    private String userid;
    private Long size;
    private Long creationDateTime;
    private Long modificationDateTime;

    public DeleteFileRequest create() {
        DeleteFileRequest deleteFileRequest = new DeleteFileRequest();
        deleteFileRequest.setId(id);
        deleteFileRequest.setUserid(userid);
        deleteFileRequest.setSize(size);
        deleteFileRequest.setCreationDateTime(creationDateTime);
        deleteFileRequest.setModificationDateTime(modificationDateTime);
        return deleteFileRequest;
    }

    public DeleteFileRequestBuilder id(String id) {
        this.id = id;
        return this;
    }

    public DeleteFileRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public DeleteFileRequestBuilder size(long size) {
        this.size = size;
        return this;
    }

    public DeleteFileRequestBuilder creationDateTime(long creationDateTime) {
        this.creationDateTime = creationDateTime;
        return this;
    }

    public DeleteFileRequestBuilder modificationDateTime(long modificationDateTime) {
        this.modificationDateTime = modificationDateTime;
        return this;
    }
}
