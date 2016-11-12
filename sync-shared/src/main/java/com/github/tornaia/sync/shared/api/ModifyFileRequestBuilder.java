package com.github.tornaia.sync.shared.api;

public class ModifyFileRequestBuilder {

    private String userid;
    private Long oldSize;
    private Long oldCreationDateTime;
    private Long oldModificationDateTime;
    private Long newSize;
    private Long newCreationDateTime;
    private Long newModificationDateTime;

    public ModifyFileRequest create() {
        ModifyFileRequest modifyFileRequest = new ModifyFileRequest();
        modifyFileRequest.setUserid(userid);
        modifyFileRequest.setOldSize(oldSize);
        modifyFileRequest.setOldCreationDateTime(oldCreationDateTime);
        modifyFileRequest.setOldModificationDateTime(oldModificationDateTime);
        modifyFileRequest.setNewSize(newSize);
        modifyFileRequest.setNewCreationDateTime(newCreationDateTime);
        modifyFileRequest.setNewModificationDateTime(newModificationDateTime);
        return modifyFileRequest;
    }

    public ModifyFileRequestBuilder userid(String userid) {
        this.userid = userid;
        return this;
    }

    public ModifyFileRequestBuilder oldSize(long oldSize) {
        this.oldSize = oldSize;
        return this;
    }

    public ModifyFileRequestBuilder oldCreationDateTime(long oldCreationDateTime) {
        this.oldCreationDateTime = oldCreationDateTime;
        return this;
    }

    public ModifyFileRequestBuilder oldModificationDateTime(long oldModificationDateTime) {
        this.oldModificationDateTime = oldModificationDateTime;
        return this;
    }

    public ModifyFileRequestBuilder newSize(long newSize) {
        this.newSize = newSize;
        return this;
    }

    public ModifyFileRequestBuilder newCreationDateTime(long newCreationDateTime) {
        this.newCreationDateTime = newCreationDateTime;
        return this;
    }

    public ModifyFileRequestBuilder newModificationDateTime(long newModificationDateTime) {
        this.newModificationDateTime = newModificationDateTime;
        return this;
    }
}
