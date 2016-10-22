package com.github.tornaia.sync.shared.api;

public class CreateFileRequestBuilder {

    private String userid;
    private String relativePath;
    private Long size;
    private Long creationDateTime;
    private Long modificationDateTime;

    public CreateFileRequest create() {
        CreateFileRequest createFileRequest = new CreateFileRequest();
        createFileRequest.setUserid(userid);
        createFileRequest.setRelativePath(relativePath);
        createFileRequest.setSize(size);
        createFileRequest.setCreationDateTime(creationDateTime);
        createFileRequest.setModificationDateTime(modificationDateTime);
        return createFileRequest;
    }

    public CreateFileRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public CreateFileRequestBuilder relativePath(String relativePath) {
        this.relativePath = relativePath;
        return this;
    }

    public CreateFileRequestBuilder size(long size) {
        this.size = size;
        return this;
    }

    public CreateFileRequestBuilder creationDateTime(long creationDateTime) {
        this.creationDateTime = creationDateTime;
        return this;
    }

    public CreateFileRequestBuilder modificationDateTime(long modificationDateTime) {
        this.modificationDateTime = modificationDateTime;
        return this;
    }
}
