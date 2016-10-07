package com.github.tornaia.sync.shared.api;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/**
 * Created by Bence on 07-Oct-16.
 */
public abstract class FileModificationRequest extends AbstractRequest {

    @NotNull
    protected Long creationDateTime;

    @NotNull
    protected Long modificationDateTime;

    @NotNull
    protected MultipartFile multipartFile;

    public long getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(Long creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public long getModificationDateTime() {
        return modificationDateTime;
    }

    public void setModificationDateTime(Long modificationDateTime) {
        this.modificationDateTime = modificationDateTime;
    }

    public MultipartFile getMultipartFile() {
        return multipartFile;
    }

    public void setMultipartFile(MultipartFile multipartFile) {
        this.multipartFile = multipartFile;
    }
}
