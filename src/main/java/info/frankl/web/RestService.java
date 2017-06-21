package info.frankl.web;

import com.google.common.eventbus.EventBus;
import info.frankl.event.MessageEvent;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("message")
public class RestService extends ResourceConfig {

  private static final Logger logger = LoggerFactory.getLogger(RestService.class);

  private EventBus eventBus;


  public RestService(final EventBus eventBus) {
    this.eventBus = eventBus;
    logger.debug("init");

  }

  /* Preflight request to allow cross origin */
  @OPTIONS
  @Produces({MediaType.TEXT_PLAIN})
  public Response get() {
    logger.debug("preflight cross origin request");
    Response response = Response.status(200).entity("ok").header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST").header("Access-Control-Allow-Headers", "Content-Type").build();
    return response;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.TEXT_PLAIN})
  public Response message(Message message) {


    logger.debug("eventbus {}", eventBus);
//    eventBus.post(new MessageEvent("9288ec3b-c32c-482d-b9a1-06b08df9eaba", "Das ist ein test"));
    eventBus.post(new MessageEvent(message.getTarget(), message.getMessage()));

    Response response = Response.status(200).entity("ok").header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST").header("Access-Control-Allow-Headers", "Content-Type").build();

    return response;
  }

  @GET
  @Path("ping")
  public Response ping() {
    return Response.status(200).build();
  }
}
