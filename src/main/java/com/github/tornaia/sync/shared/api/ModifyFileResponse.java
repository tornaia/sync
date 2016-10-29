package com.github.tornaia.sync.shared.api;

public class ModifyFileResponse {

    public enum Status {
        OK,
        NOT_FOUND,
        OUTDATED,
        TRANSFER_FAILED,
        UNKNOWN_PROBLEM
    }

    public final Status status;

    public final FileMetaInfo fileMetaInfo;

    public final String message;

    public ModifyFileResponse() {
        status = null;
        fileMetaInfo = null;
        message = null;
    }

    public ModifyFileResponse(Status status, FileMetaInfo fileMetaInfo, String message) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
        this.message = message;
    }

    public static ModifyFileResponse ok(FileMetaInfo fileMetaInfo) {
        return new ModifyFileResponse(Status.OK, fileMetaInfo, null);
    }

    public static ModifyFileResponse notFound(String message) {
        return new ModifyFileResponse(Status.NOT_FOUND, null, message);
    }

    public static ModifyFileResponse outdated(String message) {
        return new ModifyFileResponse(Status.OUTDATED, null, message);
    }

    public static ModifyFileResponse transferFailed(String message) {
        return new ModifyFileResponse(Status.TRANSFER_FAILED, null, message);
    }

    public static ModifyFileResponse unknownProblem() {
        return new ModifyFileResponse(Status.UNKNOWN_PROBLEM, null, null);
    }
}