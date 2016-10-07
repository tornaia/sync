package com.github.tornaia.sync.shared.api;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Bence on 07-Oct-16.
 */
public class UpdateFileRequest extends FileModificationRequest {

    public UpdateFileRequest(String userId, long creationDateTime, long modificationDateTime, MultipartFile multipartFile){
        this.userId = userId;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
        this.multipartFile = multipartFile;
    }

}
