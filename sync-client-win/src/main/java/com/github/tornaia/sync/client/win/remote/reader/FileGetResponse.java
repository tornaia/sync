package com.github.tornaia.sync.client.win.remote.reader;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

public class FileGetResponse {

    public enum Status {
        OK,
        NOT_FOUND,
        TRANSFER_FAILED
    }

    public final Status status;

    public final FileMetaInfo fileMetaInfo;

    public final byte[] content;

    public final String message;

    public FileGetResponse(Status status, FileMetaInfo fileMetaInfo, byte[] content, String message) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
        this.content = content;
        this.message = message;
    }

    public static FileGetResponse ok(FileMetaInfo fileMetaInfo, byte[] content) {
        return new FileGetResponse(Status.OK, fileMetaInfo, content, null);
    }

    public static FileGetResponse notFound(FileMetaInfo fileMetaInfo) {
        return new FileGetResponse(Status.NOT_FOUND, fileMetaInfo, null, null);
    }

    public static FileGetResponse transferFailed(FileMetaInfo fileMetaInfo, String message) {
        return new FileGetResponse(Status.TRANSFER_FAILED, fileMetaInfo, null, message);
    }
}
