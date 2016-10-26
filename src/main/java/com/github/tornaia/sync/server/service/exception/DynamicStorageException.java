package com.github.tornaia.sync.server.service.exception;

import com.amazonaws.AmazonClientException;

public class DynamicStorageException extends RuntimeException {

    public DynamicStorageException(Exception e) {
        super(e);
    }
}
