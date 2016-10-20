package utils;

import config.IntTestMongoConfiguration;
import config.IntTestS3Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.rules.ExpectedException.none;

@RunWith(SpringRunner.class)
@ActiveProfiles({"fongo", "findify-s3"})
@SpringBootTest(classes = {IntTestMongoConfiguration.class, IntTestS3Configuration.class})
public abstract class AbstractSyncServerIntTest {

    @Rule
    public ExpectedException expectedException = none();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Before
    public void cleanMongoDB() {
        mongoTemplate.getCollectionNames().forEach(mongoTemplate::dropCollection);
    }
}
