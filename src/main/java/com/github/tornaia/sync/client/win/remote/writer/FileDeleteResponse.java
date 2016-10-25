package com.github.tornaia.sync.client.win.remote.writer;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

public class FileDeleteResponse {

    public enum Status {
        OK,
        NOT_FOUND,
        CONFLICT,
        TRANSFER_FAILED
    }

    public final Status status;

    public final FileMetaInfo fileMetaInfo;

    public final String message;

    public FileDeleteResponse(Status status, FileMetaInfo fileMetaInfo, String message) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
        this.message = message;
    }

    public static FileDeleteResponse ok(FileMetaInfo fileMetaInfo) {
        return new FileDeleteResponse(Status.OK, fileMetaInfo, null);
    }

    public static FileDeleteResponse notFound(FileMetaInfo fileMetaInfo, String message) {
        return new FileDeleteResponse(Status.NOT_FOUND, fileMetaInfo, message);
    }

    public static FileDeleteResponse conflict(FileMetaInfo fileMetaInfo, String message) {
        return new FileDeleteResponse(Status.CONFLICT, fileMetaInfo, message);
    }

    public static FileDeleteResponse transferFailed(FileMetaInfo fileMetaInfo, String message) {
        return new FileDeleteResponse(Status.TRANSFER_FAILED, fileMetaInfo, message);
    }
}