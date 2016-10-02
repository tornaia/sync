package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

public class SyncResult {

    public enum Status {
        OK,
        CONFLICT,
        TRANSFER_FAILED
    }

    public final FileMetaInfo fileMetaInfo;
    public final Status status;

    public SyncResult(Status status, FileMetaInfo fileMetaInfo) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
    }

    public static SyncResult ok(FileMetaInfo fileMetaInfo) {
        return new SyncResult(Status.OK, fileMetaInfo);
    }

    public static SyncResult conflict(FileMetaInfo fileMetaInfo) {
        return new SyncResult(Status.CONFLICT, fileMetaInfo);
    }

    public static SyncResult terminated(FileMetaInfo fileMetaInfo) {
        return new SyncResult(Status.TRANSFER_FAILED, null);
    }
}
