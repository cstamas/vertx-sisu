package org.cstamas.vertx.sisu.examples;

import javax.inject.Inject;
import javax.inject.Named;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Named
public class ExampleVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ExampleVerticle.class);

  public static final String ADDR = "example1";

  private final MyComponent myComponent;

  @Inject
  public ExampleVerticle(MyComponent myComponent)
  {
    this.myComponent = myComponent;
  }

  @Override
  public void start() throws Exception {
    log.info("Starting " + getClass().getSimpleName());
    vertx.eventBus().consumer(
        ADDR,
        (Message<Object> event) -> {
          event.reply(myComponent.getReply());
        }
    );
  }

  @Override
  public void stop() throws Exception {
    log.info("Stopping " + getClass().getSimpleName());
  }
}
