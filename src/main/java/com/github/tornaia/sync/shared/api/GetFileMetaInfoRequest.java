package com.github.tornaia.sync.shared.api;

/**
 * Created by Bence on 07-Oct-16.
 */
public class GetFileMetaInfoRequest extends AbstractRequest {

    public GetFileMetaInfoRequest() {
    }

    public GetFileMetaInfoRequest(String userId) {
        this.userId = userId;
    }
}
