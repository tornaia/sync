package com.github.tornaia.sync.shared.api;

import javax.validation.constraints.NotNull;

/**
 * Created by Bence on 07-Oct-16.
 */
public class GetModifiedFilesRequest extends AbstractRequest {

    @NotNull
    private Long modTs;

    public GetModifiedFilesRequest() {
    }

    public GetModifiedFilesRequest(String userId, Long modTs) {
        this.userId = userId;
        this.modTs = modTs;
    }

    public long getModTs() {
        return modTs;
    }

    public void setModTs(Long modTs) {
        this.modTs = modTs;
    }
}
