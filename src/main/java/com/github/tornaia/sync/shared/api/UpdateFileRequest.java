package com.github.tornaia.sync.shared.api;

public class UpdateFileRequest extends FileModificationRequest {

    // TODO if its used by only tests then create a builder in the test folder and do not add production code only used by tests
    // and if so then the default constructor is not needed any more to be declared explicitly
    public UpdateFileRequest() {
    }

    // TODO if its used by only tests then create a builder in the test folder and do not add production code only used by tests
    // and if so then the default constructor is not needed any more to be declared explicitly
    public UpdateFileRequest(String userId, long creationDateTime, long modificationDateTime) {
        this.userId = userId;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }

}
