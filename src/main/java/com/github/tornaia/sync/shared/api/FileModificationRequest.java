package com.github.tornaia.sync.shared.api;

import javax.validation.constraints.NotNull;

public abstract class FileModificationRequest extends AbstractRequest {

    // TODO if its used by only tests then create a builder in the test folder and do not add production code only used by tests
    // and if so then the default constructor is not needed any more to be declared explicitly
    public FileModificationRequest() {
    }

    // TODO are we sure that it is really used? what about using long instead of Long
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
