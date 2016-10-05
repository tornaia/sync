package utils;

import config.IntTestMongoConfiguration;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest(classes = {IntTestMongoConfiguration.class})
public abstract class AbstractSyncServerIntTest {

  @Autowired
  protected MongoTemplate mongoTemplate;

  @After
  public void cleanMongoDB() {
    mongoTemplate.getCollectionNames().forEach(mongoTemplate::dropCollection);
  }
}
