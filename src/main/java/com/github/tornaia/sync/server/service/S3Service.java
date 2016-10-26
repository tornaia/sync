package com.github.tornaia.sync.server.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.github.tornaia.sync.server.service.exception.DynamicStorageException;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class S3Service {

    private static final Logger LOG = LoggerFactory.getLogger(S3Service.class);

    @Value("${s3.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3Client;

    public InputStream get(String id) {
        try {
            S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, id));
            return object.getObjectContent();
        } catch (AmazonClientException e) {
            LOG.warn("Communication problem with the dynamic storage", e);
            throw new DynamicStorageException(e);
        }
    }

    public void putFile(FileMetaInfo fileMetaInfo, InputStream inputStream) {
        String keyName = fileMetaInfo.id;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(fileMetaInfo.size);
        try {
            s3Client.putObject(new PutObjectRequest(bucketName, keyName, inputStream, objectMetadata));
        } catch (AmazonClientException e) {
            LOG.warn("Communication problem with the dynamic storage", e);
            throw new DynamicStorageException(e);
        }
    }

    public void deleteFile(FileMetaInfo fileMetaInfo) {
        String keyName = fileMetaInfo.id;
        try {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
        } catch (AmazonClientException e) {
            LOG.warn("Communication problem with the dynamic storage", e);
            throw new DynamicStorageException(e);
        }
    }
}
