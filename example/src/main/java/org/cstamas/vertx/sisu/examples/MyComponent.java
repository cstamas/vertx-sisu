package org.cstamas.vertx.sisu.examples;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.vertx.core.Vertx;

@Named
@Singleton
public class MyComponent
{
  private final Vertx vertx;

  @Inject
  public MyComponent(final Vertx vertx)
  {
    this.vertx = vertx;
  }

  public String getReply() {
    return "Hello " + vertx.getClass().getSimpleName();
  }
}
