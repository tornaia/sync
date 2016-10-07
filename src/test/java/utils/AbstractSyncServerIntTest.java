package utils;

import config.IntTestMongoConfiguration;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.rules.ExpectedException.none;

@SpringBootTest(classes = {IntTestMongoConfiguration.class})
public abstract class AbstractSyncServerIntTest {

  @Rule
  public ExpectedException expectedException = none();

  @Autowired
  protected MongoTemplate mongoTemplate;

  @After
  public void cleanMongoDB() {
    mongoTemplate.getCollectionNames().forEach(mongoTemplate::dropCollection);
  }
}
