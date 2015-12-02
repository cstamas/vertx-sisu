package org.cstamas.vertx.sisu;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.cstamas.vertx.sisu.examples.ExampleNamedVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SisuVerticleFactoryTest
{
  private Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext testContext) {
    vertx.close(testContext.asyncAssertSuccess());
  }

  @Test
  public void byClass(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.class.getName(),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR)
    );
  }

  @Test
  public void byName(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.NAME,
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR)
    );
  }

  private Handler<AsyncResult<String>> verifyAddressIsAliveHandler(final TestContext testContext,
                                                                   final String address)
  {
    Async async = testContext.async();
    return result -> {
      try {
        if (result.succeeded()) {
          vertx.eventBus().<String>send(address, null, r -> {
            if (r.failed()) {
              testContext.fail(r.cause());
            }
            else {
              testContext.assertEquals("Hello VertxImpl", r.result().body());
            }
          });
        }
        else {
          testContext.fail(result.cause());
        }
      }
      finally {
        async.complete();
      }
    };
  }
}
