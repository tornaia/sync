package com.github.tornaia.sync.shared.api;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public abstract class AbstractRequest implements Serializable {

    // are we sure that it is really used?
    @NotNull
    protected String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
