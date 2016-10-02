package com.github.tornaia.sync.server.data.config;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

@Configuration
public class SpringMongoConfig extends AbstractMongoConfiguration {

    private static final String COLLECTION_NAME = "sync-collection";
    private static final String DATABASE_NAME = "sync-database";
    private static final int PORT = 27017;
    private static final String HOST = "localhost";

    @Override
    protected String getDatabaseName() {
        return DATABASE_NAME;
    }

    private boolean resetDbOnStart = false;

    @Bean
    @Override
    public Mongo mongo() throws Exception {
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
}
