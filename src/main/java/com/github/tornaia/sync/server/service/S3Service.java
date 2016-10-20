package com.github.tornaia.sync.server.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.github.tornaia.sync.server.data.config.SpringS3Config;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class S3Service {

    private static final Logger LOG = LoggerFactory.getLogger(SpringS3Config.class);

    @Value("${s3.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3Client;

    public InputStream get(String id) {
        S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, id));
        S3ObjectInputStream objectContent = object.getObjectContent();
        return objectContent;
    }

    public void putFile(FileMetaInfo fileMetaInfo, InputStream inputStream) {
        String keyName = fileMetaInfo.id;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(fileMetaInfo.size);
        s3Client.putObject(new PutObjectRequest(bucketName, keyName, inputStream, objectMetadata));
    }

    public void deleteFile(FileMetaInfo fileMetaInfo) {
        String keyName = fileMetaInfo.id;
        s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
    }
}
