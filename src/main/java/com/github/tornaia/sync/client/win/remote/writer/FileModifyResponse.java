package com.github.tornaia.sync.client.win.remote.writer;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

public class FileModifyResponse {

    public enum Status {
        OK,
        CONFLICT,
        NOT_FOUND,
        TRANSFER_FAILED
    }

    public final Status status;

    public final FileMetaInfo fileMetaInfo;

    public final String message;

    public FileModifyResponse(Status status, FileMetaInfo fileMetaInfo, String message) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
        this.message = message;
    }

    public static FileModifyResponse ok(FileMetaInfo fileMetaInfo) {
        return new FileModifyResponse(Status.OK, fileMetaInfo, null);
    }

    public static FileModifyResponse conflict(FileMetaInfo fileMetaInfo) {
        return new FileModifyResponse(Status.CONFLICT, fileMetaInfo, null);
    }

    public static FileModifyResponse notFound(FileMetaInfo fileMetaInfo) {
        return new FileModifyResponse(Status.NOT_FOUND, fileMetaInfo, null);
    }

    public static FileModifyResponse transferFailed(FileMetaInfo fileMetaInfo, String message) {
        return new FileModifyResponse(Status.TRANSFER_FAILED, fileMetaInfo, message);
    }
}