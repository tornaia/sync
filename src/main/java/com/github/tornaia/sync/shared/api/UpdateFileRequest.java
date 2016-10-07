package com.github.tornaia.sync.shared.api;

/**
 * Created by Bence on 07-Oct-16.
 */
public class UpdateFileRequest extends FileModificationRequest {

    public UpdateFileRequest() {
    }

    public UpdateFileRequest(String userId, long creationDateTime, long modificationDateTime){
        this.userId = userId;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }

}
