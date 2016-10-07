package com.github.tornaia.sync.shared.api;

/**
 * Created by Bence on 07-Oct-16.
 */
public class CreateFileRequest extends FileModificationRequest {

    public CreateFileRequest(){}

    public CreateFileRequest(String userId, long creationDateTime, long modificationDateTime){
        this.userId = userId;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }
}
