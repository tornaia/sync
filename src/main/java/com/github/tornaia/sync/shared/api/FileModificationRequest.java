package com.github.tornaia.sync.shared.api;

import javax.validation.constraints.NotNull;

/**
 * Created by Bence on 07-Oct-16.
 */
public abstract class FileModificationRequest extends AbstractRequest {

    public FileModificationRequest(){}

    public FileModificationRequest(Long creationDateTime, Long modificationDateTime) {
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }

    @NotNull
    protected Long creationDateTime;

    @NotNull
    protected Long modificationDateTime;


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

}
