package com.github.tornaia.sync.server.service.exception;

public class DirectoryNotEmptyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DirectoryNotEmptyException(String path) {
        super("Directory is not empty: " + path);
    }
}
