package com.github.tornaia.sync.server.data.config;

import com.github.tornaia.sync.shared.util.SerializerUtils;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
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
import java.util.Objects;

@Configuration
public class SpringMongoConfig extends AbstractMongoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringMongoConfig.class);

    @Value("${VCAP_SERVICES:#{null}}")
    private String vcapServices;

    @Value("${mongo.reset.on.start:false}")
    private boolean resetOnStart;

    @Value("${mongo.local.development.host:localhost}")
    private String host;

    @Value("${mongo.local.development.port:27017}")
    private int port;

    @Value("${mongo.local.development.database.name:sync-database}")
    private String databaseName;

    @Value("${mongo.local.development.collection.name:sync-collection}")
    private String collectionName;

    @Autowired
    private SerializerUtils serializerUtils;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Bean
    @Override
    public Mongo mongo() throws Exception {
        LOG.info("vcapServices: " + vcapServices);
        boolean isCloud = !Objects.isNull(vcapServices);
        if (isCloud) {
            return initCloud();
        } else {
            return initLocal();
        }
    }

    private MongoClient initCloud() throws IOException {
        LOG.info("We are in cloud. Init Mongo for Cloud!");
        LOG.info("Reset on start? " + resetOnStart);
        LOG.trace("vcapServices: " + vcapServices);
        Map<String, Object> vcapServicesMap = serializerUtils.toObject(vcapServices, Map.class);
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

    private MongoClient initLocal() {
        LOG.info("Local development. Init Mongo for Local!");
        LOG.info("Reset on start? " + resetOnStart);
        MongoClient mongoClient = new MongoClient(host, port);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);
        if (resetOnStart) {
            mongoDatabase.drop();
        }
        mongoDatabase = mongoClient.getDatabase(databaseName);
        if (resetOnStart) {
            mongoDatabase.createCollection(collectionName);
        }
        return mongoClient;
    }
}
