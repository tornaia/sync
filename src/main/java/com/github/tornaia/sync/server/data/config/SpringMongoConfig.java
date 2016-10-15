package com.github.tornaia.sync.server.data.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // TODO use @Value
    private static final String VCAP_SERVICES = "VCAP_SERVICES";

    // TODO use @Value
    private String HOST = "localhost";
    private int PORT = 27017;
    private String DATABASE_NAME = "sync-database";

    private String COLLECTION_NAME = "sync-collection";

    // TODO use @Value
    private boolean resetDbOnStart = false;

    @Override
    protected String getDatabaseName() {
        return DATABASE_NAME;
    }


    @Bean
    @Override
    public Mongo mongo() throws Exception {
        LOG.info("Reset db on start? " + resetDbOnStart);
        if (!Objects.isNull(System.getenv(VCAP_SERVICES))) {
            LOG.info("We are in cloud! Init mongo for cloud!");
            return initCloud();
        } else {
            LOG.info("Local development. Init mongo for local!");
            return initLocal();
        }
    }

    private MongoClient initCloud() throws IOException {
        LOG.info("initCloud");
        String vcapServicesAsJson = System.getenv(VCAP_SERVICES);
        Map<String, Object> vcapServicesMap = parse(vcapServicesAsJson);
        List<Map<String, Object>> mongoDbList = (List<Map<String, Object>>) vcapServicesMap.get("mongodb");
        Map<String, Object> mongoDbFirstMap = mongoDbList.get(0);
        Map<String, String> credentialsMap = (Map<String, String>) mongoDbFirstMap.get("credentials");
        String database_uri = credentialsMap.get("database_uri");
        String database = credentialsMap.get("database");
        MongoClient mongoClient = new MongoClient(new MongoClientURI(database_uri));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
        if (resetDbOnStart) {
            mongoDatabase.drop();
        }
        mongoDatabase = mongoClient.getDatabase(database);
        if (resetDbOnStart) {
            mongoDatabase.createCollection(COLLECTION_NAME);
        }
        return mongoClient;
    }

    private MongoClient initLocal() {
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);
        if (resetDbOnStart) {
            mongoDatabase.drop();
        }
        mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);
        if (resetDbOnStart) {
            mongoDatabase.createCollection(COLLECTION_NAME);
        }
        return mongoClient;
    }

    private Map<String, Object> parse(String json) throws IOException {
        return new ObjectMapper().readValue(json, Map.class);
    }
}
