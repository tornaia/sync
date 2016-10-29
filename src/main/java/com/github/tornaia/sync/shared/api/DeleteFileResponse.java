package com.github.tornaia.sync.shared.api;

public class DeleteFileResponse {

    public enum Status {
        OK,
        NOT_FOUND,
        OUTDATED,
        TRANSFER_FAILED,
        UNKNOWN_PROBLEM
    }

    public final Status status;

    public final String message;

    public DeleteFileResponse() {
        status = null;
        message = null;
    }

    public DeleteFileResponse(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static DeleteFileResponse ok() {
        return new DeleteFileResponse(Status.OK, null);
    }

    public static DeleteFileResponse notFound(String message) {
        return new DeleteFileResponse(Status.NOT_FOUND, message);
    }

    public static DeleteFileResponse outdated(String message) {
        return new DeleteFileResponse(Status.OUTDATED, message);
    }

    public static DeleteFileResponse transferFailed(String message) {
        return new DeleteFileResponse(Status.TRANSFER_FAILED, message);
    }

    public static DeleteFileResponse unknownProblem() {
        return new DeleteFileResponse(Status.UNKNOWN_PROBLEM, null);
    }
}
