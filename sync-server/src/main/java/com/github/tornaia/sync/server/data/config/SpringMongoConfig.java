package com.github.tornaia.sync.server.data.config;

import com.github.tornaia.sync.shared.util.SerializerUtils;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class SpringMongoConfig extends AbstractMongoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringMongoConfig.class);

    @Value("${VCAP_SERVICES:#{null}}")
    private String vcapServices;

    @Value("${mongo.database.uri:#{null}}")
    private String databaseUri;

    @Value("${mongo.collection.name:#{null}}")
    private String collectionName;

    private boolean resetOnStart = false;

    @Autowired
    private SerializerUtils serializerUtils;

    @Override
    protected String getDatabaseName() {
        return StringUtils.substringAfterLast(databaseUri, "/");
    }

    @Bean
    @Override
    public Mongo mongo() throws Exception {
        boolean isCloud = vcapServices != null;
        boolean isFongo = "mongodb://fongo-so-dont-care/sync-database".equals(databaseUri);
        if (isCloud) {
            return initCloud();
        } else if (isFongo) {
            return initFongo();
        } else {
            return initLocal();
        }
    }

    private MongoClient initCloud() throws IOException {
        LOG.info("We are in cloud. Init Mongo for Cloud!");
        Optional<Map> optionalVcapServicesMap = serializerUtils.toObject(vcapServices, Map.class);
        Map<String, Object> vcapServicesMap = optionalVcapServicesMap.get();
        List<Map<String, Object>> mongoDbList = (List<Map<String, Object>>) vcapServicesMap.get("mongodb");
        Map<String, Object> mongoDbFirstMap = mongoDbList.get(0);
        Map<String, String> credentialsMap = (Map<String, String>) mongoDbFirstMap.get("credentials");
        String database_uri = credentialsMap.get("database_uri");
        String database = credentialsMap.get("database");
        MongoClient mongoClient = new MongoClient(new MongoClientURI(database_uri));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
        if (resetOnStart) {
            mongoDatabase.drop();
        }
        mongoDatabase = mongoClient.getDatabase(database);
        if (resetOnStart) {
            mongoDatabase.createCollection(collectionName);
        }
        return mongoClient;
    }

    private MongoClient initFongo() {
        LOG.info("Tests. Init Fongo for Tests!");
        return new MongoClient(new MongoClientURI(databaseUri));
    }

    private MongoClient initLocal() {
        LOG.info("Local development. Init Mongo for Local development!");
        LOG.info("Reset on start? " + resetOnStart);
        MongoClient mongoClient = new MongoClient(new MongoClientURI(StringUtils.substringBeforeLast(databaseUri, "/")));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(getDatabaseName());
        if (resetOnStart) {
            mongoDatabase.drop();
        }
        mongoDatabase = mongoClient.getDatabase(getDatabaseName());
        if (resetOnStart) {
            mongoDatabase.createCollection(collectionName);
        }
        return mongoClient;
    }
}
