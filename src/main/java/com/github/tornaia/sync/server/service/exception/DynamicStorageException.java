package com.github.tornaia.sync.server.service.exception;

import com.amazonaws.AmazonClientException;

public class DynamicStorageException extends RuntimeException {

    public DynamicStorageException(AmazonClientException e) {
        super(e);
    }
}
