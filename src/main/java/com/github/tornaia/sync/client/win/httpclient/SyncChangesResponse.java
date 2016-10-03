package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

import java.util.List;

public class SyncChangesResponse {

    public enum Status {
        OK,
        TRANSFER_FAILED
    }

    public final Status status;

    public final List<FileMetaInfo> fileMetaInfos;

    public final String reason;

    public SyncChangesResponse(Status status, List<FileMetaInfo> fileMetaInfos, String reason) {
        this.status = status;
        this.fileMetaInfos = fileMetaInfos;
        this.reason = reason;
    }

    public static SyncChangesResponse ok(List<FileMetaInfo> fileMetaInfos) {
        return new SyncChangesResponse(Status.OK, fileMetaInfos, null);
    }

    public static SyncChangesResponse transferFailed(String reason) {
        return new SyncChangesResponse(Status.TRANSFER_FAILED, null, reason);
    }
}
