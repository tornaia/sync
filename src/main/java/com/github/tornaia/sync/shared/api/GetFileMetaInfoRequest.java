package com.github.tornaia.sync.shared.api;

public class GetFileMetaInfoRequest extends AbstractRequest {

    public GetFileMetaInfoRequest() {
    }

    // TODO if its used by only tests then create a builder in the test folder and do not add production code only used by tests
    // and if so then the default constructor is not needed any more to be declared explicitly
    public GetFileMetaInfoRequest(String userId) {
        this.userId = userId;
    }
}
