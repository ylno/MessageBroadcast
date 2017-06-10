package info.frankl.web;

import com.google.common.eventbus.EventBus;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application class for this example.
 *
 * @author Martin Matula
 */
public class JaxRsApplication extends ResourceConfig {

  public JaxRsApplication(final EventBus eventBus) {

    register(new RestService(eventBus));

  }

}
