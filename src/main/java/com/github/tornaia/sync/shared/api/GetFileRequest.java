package com.github.tornaia.sync.shared.api;

/**
 * Created by Bence on 07-Oct-16.
 */
public class GetFileRequest extends AbstractRequest {

    public GetFileRequest(String userId) {
        this.userId = userId;
    }
}
