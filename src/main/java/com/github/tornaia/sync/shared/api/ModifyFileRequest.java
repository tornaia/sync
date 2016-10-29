package com.github.tornaia.sync.shared.api;

public class ModifyFileRequest extends AbstractRequest {

    private String userid;

    private long oldSize;

    private long oldCreationDateTime;

    private long oldModificationDateTime;

    private long newSize;

    private long newCreationDateTime;

    private long newModificationDateTime;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public long getOldSize() {
        return oldSize;
    }

    public void setOldSize(long oldSize) {
        this.oldSize = oldSize;
    }

    public long getOldCreationDateTime() {
        return oldCreationDateTime;
    }

    public void setOldCreationDateTime(long oldCreationDateTime) {
        this.oldCreationDateTime = oldCreationDateTime;
    }

    public long getOldModificationDateTime() {
        return oldModificationDateTime;
    }

    public void setOldModificationDateTime(long oldModificationDateTime) {
        this.oldModificationDateTime = oldModificationDateTime;
    }

    public long getNewSize() {
        return newSize;
    }

    public void setNewSize(long newSize) {
        this.newSize = newSize;
    }

    public long getNewCreationDateTime() {
        return newCreationDateTime;
    }

    public void setNewCreationDateTime(long newCreationDateTime) {
        this.newCreationDateTime = newCreationDateTime;
    }

    public long getNewModificationDateTime() {
        return newModificationDateTime;
    }

    public void setNewModificationDateTime(long newModificationDateTime) {
        this.newModificationDateTime = newModificationDateTime;
    }
}
