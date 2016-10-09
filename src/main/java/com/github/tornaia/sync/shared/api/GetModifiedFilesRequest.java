package com.github.tornaia.sync.shared.api;

public class GetModifiedFilesRequest extends AbstractRequest {

    private String userid;
    private long modTs;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public long getModTs() {
        return modTs;
    }

    public void setModTs(long modTs) {
        this.modTs = modTs;
    }
}
