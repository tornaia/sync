package com.github.tornaia.sync.server.service.exception;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public class DynamicStorageException extends RuntimeException {

    public DynamicStorageException(Exception e) {
        super(e);
    }

    public DynamicStorageException(String message, AmazonS3Exception e) {
        super(message, e);
    }
}
