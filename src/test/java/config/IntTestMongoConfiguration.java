package config;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class IntTestMongoConfiguration {

    @Bean
    public Mongo mongo() {
        return new Fongo("sync-server-int-db").getMongo();
    }
}
