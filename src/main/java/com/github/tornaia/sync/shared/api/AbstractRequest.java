package com.github.tornaia.sync.shared.api;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Bence on 07-Oct-16.
 */
public abstract class AbstractRequest implements Serializable {

    @NotNull
    protected String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}