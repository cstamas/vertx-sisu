package org.cstamas.vertx.sisu;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SisuLocalVerticleFactoryTest
{
  private Vertx vertx;

  @Before
  public void setUp(TestContext context) throws Exception {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void verifyBothDeployed(TestContext testContext) {
    vertx.deployVerticle("sisu-local:bootstrap", verifyBothDeployedHandler(testContext));
  }

  @Test
  public void verifyEndsWithFilter(TestContext testContext) {
    vertx.deployVerticle("sisu-local:bootstrap::*NamedVerticle", verifyExample2DeployedOnlyHandler(testContext));
  }

  @Test
  public void verifyStartsWithFilter(TestContext testContext) {
    vertx.deployVerticle("sisu-local:bootstrap::ExampleNamed*", verifyExample2DeployedOnlyHandler(testContext));
  }

  @Test
  public void verifyEqualsFilter(TestContext testContext) {
    vertx.deployVerticle("sisu-local:bootstrap::ExampleNamedVerticle", verifyExample2DeployedOnlyHandler(testContext));
  }

  private Handler<AsyncResult<String>> verifyBothDeployedHandler(final TestContext testContext) {
    Async async = testContext.async();
    return result -> {
      try {
        if (result.succeeded()) {
          vertx.eventBus().<String>send("example1", null, r -> {
            if (r.failed()) {
              testContext.fail(r.cause());
            }
            testContext.assertEquals("Hello VertxImpl", r.result().body());
          });

          vertx.eventBus().<String>send("example2", null, r -> {
            if (r.failed()) {
              testContext.fail(r.cause());
            }
            testContext.assertEquals("Hello VertxImpl", r.result().body());
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

  private Handler<AsyncResult<String>> verifyExample2DeployedOnlyHandler(final TestContext testContext) {
    Async async = testContext.async();
    return result -> {
      try {
        if (result.succeeded()) {
          vertx.eventBus().<String>send("example1", null, r -> {
            if (r.failed()) {
              // good
            }
            else {
              testContext.fail("Should not start up ExampleVericle");
            }
          });

          vertx.eventBus().<String>send("example2", null, r -> {
            if (r.failed()) {
              testContext.fail(r.cause());
            }
            testContext.assertEquals("Hello VertxImpl", r.result().body());
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
