package info.frankl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executors;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.google.common.eventbus.EventBus;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import info.frankl.bots.KonvBot;
import info.frankl.dao.ChatDAO;
import info.frankl.service.DataService;
import info.frankl.web.JaxRsApplication;

public class ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);

  public static final String BOT_NAME = "bot-name";

  public static final String TELEGRAM_BOT_KEY = "telegram-bot-key";

  public static final String REDIS_HOST = "redis-host";

  public static void main(String[] args) {

    EventBus eventBus = new EventBus();

    logger.debug("start");
    TelegramBotsApi telegramBotsApi = null;
    try {
      telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
    }
    catch (TelegramApiException e) {
      e.printStackTrace();
    }

    Properties properties = Botpropperties.readProperties();

    checkProperties(properties);

    try {
      final ChatDAO chatDao = new ChatDAO(properties.getProperty(REDIS_HOST));
      final DataService dataService = new DataService(chatDao);
      KonvBot bot = new KonvBot(eventBus, properties.getProperty(TELEGRAM_BOT_KEY), properties.getProperty(BOT_NAME), dataService);
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
    checkProperty(properties, REDIS_HOST);
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
    return UriBuilder.fromUri("http://0.0.0.0/").port(8000).build();
  }

}
