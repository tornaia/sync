package com.github.tornaia.sync.shared.api;

public class CreateFileRequest extends FileModificationRequest {

    public CreateFileRequest() {
    }

    public CreateFileRequest(String userId, long creationDateTime, long modificationDateTime) {
        this.userId = userId;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }
}
