package info.frankl.web;

import com.google.common.eventbus.EventBus;
import info.frankl.event.MessageEvent;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("message")
public class RestService extends ResourceConfig {

  private static final Logger logger = LoggerFactory.getLogger(RestService.class);

  private EventBus eventBus;

  public RestService(final EventBus eventBus) {
    this.eventBus = eventBus;
    logger.debug("init");

  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.TEXT_PLAIN})
  public String message(Message message) {
    logger.debug("eventbus {}", eventBus);
//    eventBus.post(new MessageEvent("9288ec3b-c32c-482d-b9a1-06b08df9eaba", "Das ist ein test"));
    eventBus.post(new MessageEvent(message.getTarget(), message.getMessage()));
    return "Yea! ";
  }
}
