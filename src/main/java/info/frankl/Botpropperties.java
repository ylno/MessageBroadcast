package info.frankl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Botpropperties {

  private static final Logger logger = LoggerFactory.getLogger(Botpropperties.class);

  public static Properties readProperties() {
    Properties properties = new Properties();
    InputStream input = null;

    try {

      String filename = "settings.properties";
      input = Botpropperties.class.getClassLoader().getResourceAsStream(filename);
      if (input == null) {
        logger.error("Sorry, unable to find " + filename);
        return null;
      }

      //load a properties file from class path, inside static method
      properties.load(input);

    } catch (IOException ex) {
      logger.debug("error loading properties: {}", ex);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return properties;
  }
}
