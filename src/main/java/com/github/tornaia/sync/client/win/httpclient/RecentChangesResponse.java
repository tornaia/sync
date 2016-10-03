package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

import java.util.List;

public class RecentChangesResponse {

    public enum Status {
        OK,
        TRANSFER_FAILED
    }

    public final Status status;

    public final List<FileMetaInfo> fileMetaInfos;

    public final String reason;

    public RecentChangesResponse(Status status, List<FileMetaInfo> fileMetaInfos, String reason) {
        this.status = status;
        this.fileMetaInfos = fileMetaInfos;
        this.reason = reason;
    }

    public static RecentChangesResponse ok(List<FileMetaInfo> fileMetaInfos) {
        return new RecentChangesResponse(Status.OK, fileMetaInfos, null);
    }

    public static RecentChangesResponse transferFailed(String reason) {
        return new RecentChangesResponse(Status.TRANSFER_FAILED, null, reason);
    }
}
