package info.frankl;

import com.google.common.eventbus.EventBus;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import info.frankl.bots.KonvBot;
import info.frankl.web.JaxRsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executors;

public class ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);

  public static final String BOT_NAME = "bot-name";

  public static final String BOT_PATH = "bot-path";

  public static final String TELEGRAM_BOT_KEY = "telegram-bot-key";

  public static final String LANGUAGE = "language";

  public static final String COUNTRY = "country";

  public static void main(String[] args) {

    EventBus eventBus = new EventBus();

    logger.debug("start");
    ApiContextInitializer.init();
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

    Properties properties = Botpropperties.readProperties();

    checkProperties(properties);

    try {
      KonvBot bot = new KonvBot(eventBus, properties.getProperty("telegram-bot-key"), properties.getProperty("bot-name"));
      telegramBotsApi.registerBot(bot);
      eventBus.register(bot);

    } catch (TelegramApiException e) {
      e.printStackTrace();
    }

    try {
      logger.debug("start http service");
      startServer(eventBus);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private static void checkProperties(final Properties properties) {
    checkProperty(properties, BOT_NAME);
    checkProperty(properties, TELEGRAM_BOT_KEY);
  }

  private static void checkProperty(final Properties properties, final String parameterName) {
    if (properties.getProperty(parameterName) == null || properties.getProperty(parameterName).isEmpty()) {
      logger.debug("property exception");
      throw new RuntimeException("Parameter missing: " + parameterName + ". Please define it in settings.properties");
    }
  }

  static HttpServer startServer(final EventBus eventBus) throws IOException {
    // create a new server listening at port 8080

    Executors.newFixedThreadPool(20);

    final HttpServer server = HttpServer.create(new InetSocketAddress(getBaseURI().getPort()), 0);
    server.setExecutor(Executors.newFixedThreadPool(10));
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

      @Override
      public void run() {
        server.stop(0);
      }
    }));

    // create a handler wrapping the JAX-RS application
    HttpHandler handler = RuntimeDelegate.getInstance().createEndpoint(new JaxRsApplication(eventBus), HttpHandler.class);

    // map JAX-RS handler to the server root
    server.createContext(getBaseURI().getPath(), handler);

    // start the server
    server.start();

    return server;
  }

  public static URI getBaseURI() {
    return UriBuilder.fromUri("http://localhost/").port(8000).build();
  }

}
