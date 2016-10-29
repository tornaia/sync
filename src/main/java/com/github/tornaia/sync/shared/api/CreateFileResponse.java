package com.github.tornaia.sync.shared.api;

public class CreateFileResponse {

    public enum Status {
        OK,
        ALREADY_EXIST,
        TRANSFER_FAILED,
        UNKNOWN_PROBLEM
    }

    public final Status status;

    public final FileMetaInfo fileMetaInfo;

    public final String message;

    public CreateFileResponse() {
        status = null;
        fileMetaInfo = null;
        message = null;
    }

    public CreateFileResponse(Status status, FileMetaInfo fileMetaInfo, String message) {
        this.status = status;
        this.fileMetaInfo = fileMetaInfo;
        this.message = message;
    }

    public static CreateFileResponse ok(FileMetaInfo fileMetaInfo) {
        return new CreateFileResponse(Status.OK, fileMetaInfo, null);
    }

    public static CreateFileResponse alreadyExists(String message) {
        return new CreateFileResponse(Status.ALREADY_EXIST, null, message);
    }

    public static CreateFileResponse transferFailed(String message) {
        return new CreateFileResponse(Status.TRANSFER_FAILED, null, message);
    }

    public static CreateFileResponse unknownProblem() {
        return new CreateFileResponse(Status.UNKNOWN_PROBLEM, null, null);
    }
}
