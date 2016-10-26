package com.github.tornaia.sync.server.data.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.github.tornaia.sync.shared.util.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class SpringS3Config {

    private static final Logger LOG = LoggerFactory.getLogger(SpringS3Config.class);

    @Value("${VCAP_SERVICES:#{null}}")
    private String vcapServices;

    @Value("${s3.access.host:#{null}}")
    private String accessHost;

    @Value("${s3.access.port:-1}")
    private int accessPort;

    @Value("${s3.bucket.name:#{null}}")
    private String bucketName;

    @Value("${s3.access.key:#{null}}")
    private String accessKey;

    @Value("${s3.shared.secret:#{null}}")
    private String sharedSecret;

    @Autowired
    private SerializerUtils serializerUtils;

    @Bean
    public AmazonS3 s3Client() {
        boolean isCloud = vcapServices != null;

        AmazonS3 s3Client;
        if (isCloud) {
            s3Client = initCloud();
        } else {
            s3Client = initLocal();
        }

        reCreateBucketIfNecessary(s3Client, bucketName);

        return s3Client;
    }

    private AmazonS3 initCloud() {
        LOG.info("We are in cloud. Init S3 for Cloud!");
        Map<String, Object> vcapServicesMap = serializerUtils.toObject(vcapServices, Map.class);
        List<Map<String, Object>> dynstrgList = (List<Map<String, Object>>) vcapServicesMap.get("dynstrg");
        Map<String, Object> s3StorageFirstMap = dynstrgList.get(0);
        Map<String, String> credentialsMap = (Map<String, String>) s3StorageFirstMap.get("credentials");
        String accessKey = credentialsMap.get("accessKey");
        String sharedSecret = credentialsMap.get("sharedSecret");
        String accessHost = credentialsMap.get("accessHost");

        ClientConfiguration opts = new ClientConfiguration();
        opts.setSignerOverride("S3SignerType");
        // TODO use properties (per profile)
        opts.setConnectionTimeout(600000);
        opts.setMaxConnections(50);
        opts.setSocketTimeout(600000);
        opts.setMaxErrorRetry(5);

        AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, sharedSecret), opts);
        s3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        s3Client.setEndpoint(accessHost);
        return s3Client;
    }

    private AmazonS3 initLocal() {
        LOG.info("Local development. Init S3 for Local!");
        AmazonS3Client s3Client = new AmazonS3Client(new AnonymousAWSCredentials());
        s3Client.setEndpoint(accessHost + ":" + accessPort);
        return s3Client;
    }

    private void reCreateBucketIfNecessary(AmazonS3 s3Client, String bucketName) {
        boolean bucketExist = isBucketExist(s3Client, bucketName);
        LOG.info("Bucket exists: " + bucketExist);
        if (!bucketExist) {
            LOG.info("Creating bucket: " + bucketName);
            s3Client.createBucket(new CreateBucketRequest(bucketName));
        }
    }

    private boolean isBucketExist(AmazonS3 s3Client, String bucketName) {
        return s3Client.listBuckets().stream().filter(bucket -> Objects.equals(bucketName, bucket.getName())).findFirst().isPresent();
    }
}
