package com.github.tornaia.sync.shared.exception;

import java.io.IOException;

public class SerializerException extends RuntimeException {

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Exception cause) {
        super(message, cause);
    }
}
