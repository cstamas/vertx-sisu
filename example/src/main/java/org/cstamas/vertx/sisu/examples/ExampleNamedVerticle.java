package org.cstamas.vertx.sisu.examples;

import javax.inject.Inject;
import javax.inject.Named;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;

@Named(ExampleNamedVerticle.NAME)
public class ExampleNamedVerticle
    extends AbstractVerticle
{
  public static final String NAME = "ExampleNamedVerticle";

  public static final String ADDR = "example2";

  private final MyComponent myComponent;

  private MessageConsumer messageConsumer;

  @Inject
  public ExampleNamedVerticle(MyComponent myComponent)
  {
    this.myComponent = myComponent;
  }

  @Override
  public void start() throws Exception {
    messageConsumer = vertx.eventBus().consumer(
        ADDR,
        (Message<Object> event) -> {
          event.reply(myComponent.getReply());
        }
    );
  }

  @Override
  public void stop() throws Exception {
    if (messageConsumer != null) {
      messageConsumer.unregister();
    }
  }
}
