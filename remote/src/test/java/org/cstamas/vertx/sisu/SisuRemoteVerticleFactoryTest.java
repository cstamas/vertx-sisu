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
public class SisuRemoteVerticleFactoryTest
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
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT",
        verifyBothDeployedHandler(testContext)
    );
  }

  @Test
  public void verifyEndsWithFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT::*NamedVerticle",
        verifyExample2DeployedOnlyHandler(testContext)
    );
  }

  @Test
  public void verifyStartsWithFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT::ExampleNamed*",
        verifyExample2DeployedOnlyHandler(testContext)
    );
  }

  @Test
  public void verifyEqualsFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT::ExampleNamedVerticle",
        verifyExample2DeployedOnlyHandler(testContext)
    );
  }


  private Handler<AsyncResult<String>> verifyBothDeployedHandler(final TestContext testContext) {
    Async async = testContext.async();
    return e -> {
      try {
        if (e.succeeded()) {
          vertx.eventBus().<String>send("example1", null, result -> {
            if (result.failed()) {
              testContext.fail(result.cause());
            }
            testContext.assertEquals("Hello VertxImpl", result.result().body());
          });

          vertx.eventBus().<String>send("example2", null, result -> {
            if (result.failed()) {
              testContext.fail(result.cause());
            }
            testContext.assertEquals("Hello VertxImpl", result.result().body());
          });
        }
        else {
          testContext.fail(e.cause());
        }
      }
      finally {
        async.complete();
      }
    };
  }

  private Handler<AsyncResult<String>> verifyExample2DeployedOnlyHandler(final TestContext testContext) {
    Async async = testContext.async();
    return e -> {
      try {
        if (e.succeeded()) {
          vertx.eventBus().<String>send("example1", null, result -> {
            if (result.failed()) {
              // good
            }
            else {
              testContext.fail("Should not start up ExampleVericle");
            }
          });

          vertx.eventBus().<String>send("example2", null, result -> {
            if (result.failed()) {
              testContext.fail(result.cause());
            }
            testContext.assertEquals("Hello VertxImpl", result.result().body());
          });
        }
        else {
          testContext.fail(e.cause());
        }
      }
      finally {
        async.complete();
      }
    };
  }
}
