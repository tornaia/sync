package com.github.tornaia.sync.server.service.exception;

public class FileAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileAlreadyExistsException(String path) {
        super("File already exists: " + path);
    }
}
