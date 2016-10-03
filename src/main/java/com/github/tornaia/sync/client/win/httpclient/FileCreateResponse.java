package com.github.tornaia.sync.client.win.httpclient;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

public class FileCreateResponse {

    public enum Status {
        OK,
        CONFLICT,
        TRANSFER_FAILED
    }

    public final Status status;

    public final FileMetaInfo fileMetaInfo;

    public final String message;

    public FileCreateResponse(Status status, FileMetaInfo fileMetaInfo, String message) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
        this.message = message;
    }

    public static FileCreateResponse ok(FileMetaInfo fileMetaInfo) {
        return new FileCreateResponse(Status.OK, fileMetaInfo, null);
    }

    public static FileCreateResponse conflict(FileMetaInfo fileMetaInfo) {
        return new FileCreateResponse(Status.CONFLICT, fileMetaInfo, null);
    }

    public static FileCreateResponse transferFailed(String message) {
        return new FileCreateResponse(Status.TRANSFER_FAILED, null, message);
    }
}
