package com.github.tornaia.sync.shared.api;

public enum GetFileResponseStatus {
    OK,
    NOT_FOUND,
    TRANSFER_FAILED,
    UNKNOWN_PROBLEM;

    public static final String FILE_STATUS_HEADER_FIELD_NAME = "File-Status";
}
