package info.frankl.web;

import com.google.common.eventbus.EventBus;
import org.glassfish.jersey.server.ResourceConfig;

public class JaxRsApplication extends ResourceConfig {

  public JaxRsApplication(final EventBus eventBus) {

    register(new RestService(eventBus));

  }

}
