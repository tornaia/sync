package com.github.tornaia.sync.server.service.exception;

public class FileNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileNotFoundException(String path) {
        super("Could not find file: " + path);
    }
}
