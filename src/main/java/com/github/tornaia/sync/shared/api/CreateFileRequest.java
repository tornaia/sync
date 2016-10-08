package com.github.tornaia.sync.shared.api;

// TODO Create should not inherint from modification. do not use inheritance
public class CreateFileRequest extends FileModificationRequest {

    // TODO if its used by only tests then create a builder in the test folder and do not add production code only used by tests
    // and if so then the default constructor is not needed any more to be declared explicitly
    public CreateFileRequest() {
    }

    // TODO if its used by only tests then create a builder in the test folder and do not add production code only used by tests
    public CreateFileRequest(String userId, long creationDateTime, long modificationDateTime) {
        this.userId = userId;
        this.creationDateTime = creationDateTime;
        this.modificationDateTime = modificationDateTime;
    }
}
