package org.cstamas.vertx.sisu;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.cstamas.vertx.sisu.examples.ExampleNamedVerticle;
import org.cstamas.vertx.sisu.examples.ExampleVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

@RunWith(VertxUnitRunner.class)
public class SisuLocalVerticleFactoryTest
{
  private Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void byClass(TestContext testContext) throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(
        "sisu:" + ExampleVerticle.class.getName(),
        result -> {
          if (result.succeeded()) {
            future.complete(null);
          }
          else {
            future.completeExceptionally(result.cause());
          }
        }
    );
    future.get(1, TimeUnit.SECONDS);

    vertx.eventBus().<String>send(ExampleVerticle.ADDR, null, result -> {
      if (result.failed()) {
        result.cause().printStackTrace();
        fail();
        return;
      }
      testContext.assertEquals("Hello VertxImpl", result.result().body());
    });
  }

  @Test
  public void byName(TestContext testContext) throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.NAME,
        result -> {
          if (result.succeeded()) {
            future.complete(null);
          }
          else {
            future.completeExceptionally(result.cause());
          }
        }
    );
    future.get(1, TimeUnit.SECONDS);

    vertx.eventBus().<String>send(ExampleNamedVerticle.ADDR, null, result -> {
      if (result.failed()) {
        result.cause().printStackTrace();
        fail();
        return;
      }
      testContext.assertEquals("Hello VertxImpl", result.result().body());
    });
  }
}
