package com.github.tornaia.sync.server;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan("com.github.tornaia.sync.server")
public class ServerApp {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ServerApp.class, args);
        initMongo();
    }

    public static void initMongo() {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("sync-database");
        mongoDatabase.drop();
        mongoDatabase = mongoClient.getDatabase("sync-database");
        mongoDatabase.createCollection("sync-collection");
    }
}