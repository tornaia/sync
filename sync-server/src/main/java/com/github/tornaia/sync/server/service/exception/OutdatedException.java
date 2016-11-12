package com.github.tornaia.sync.server.service.exception;

public class OutdatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OutdatedException(String userid, String id) {
        super("Could not find file! userid: " + userid + ", id: " + id);
    }

    public OutdatedException(String message, Exception e) {
        super(message, e);
    }
}
