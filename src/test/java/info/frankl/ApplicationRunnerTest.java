package info.frankl;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

public class ApplicationRunnerTest {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationRunnerTest.class);

  @Test
  public void testInit() throws Exception {
    logger.debug("test log");
  }

  @Test
  public void testFail() throws Exception {
    fail();
  }
}
