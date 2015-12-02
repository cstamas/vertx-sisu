package org.cstamas.vertx.sisu.examples;

import javax.inject.Inject;
import javax.inject.Named;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Named(ExampleNamedVerticle.NAME)
public class ExampleNamedVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(ExampleNamedVerticle.class);

  public static final String NAME = "ExampleNamedVerticle";

  public static final String ADDR = "example2";

  private final MyComponent myComponent;

  @Inject
  public ExampleNamedVerticle(MyComponent myComponent)
  {
    this.myComponent = myComponent;
  }

  @Override
  public void start() throws Exception {
    log.info("Starting");
    vertx.eventBus().consumer(
        ADDR,
        (Message<Object> event) -> {
          event.reply(myComponent.getReply());
        }
    );
  }
}
