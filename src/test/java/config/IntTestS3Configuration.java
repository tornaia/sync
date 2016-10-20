package config;

import io.findify.s3mock.S3Mock;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@TestConfiguration
public class IntTestS3Configuration {

    @Value("${s3.access.port}")
    private int accessPort;

    @Value("${s3.storage.directory}")
    private String storageDirectory;

    @PostConstruct
    public void startFindifyS3mock() throws IOException {
        File file = new File(storageDirectory);
        FileUtils.deleteDirectory(file);
        S3Mock s3Mock = S3Mock.create(accessPort, storageDirectory);
        s3Mock.start();
    }
}
