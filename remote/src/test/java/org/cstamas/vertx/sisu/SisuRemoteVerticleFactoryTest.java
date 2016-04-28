package org.cstamas.vertx.sisu;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
        verifyBothDeployedHandler(testContext, "DEFAULT")
    );
  }

  @Test
  public void verifyBothDeployedWithConfig(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT",
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "verifyBothDeployedWithConfig")),
        verifyBothDeployedHandler(testContext, "verifyBothDeployedWithConfig")
    );
  }

  @Test
  public void verifyEndsWithFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT::*NamedVerticle",
        verifyExample2DeployedOnlyHandler(testContext, "DEFAULT")
    );
  }

  @Test
  public void verifyStartsWithFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT::ExampleNamed*",
        verifyExample2DeployedOnlyHandler(testContext, "DEFAULT")
    );
  }

  @Test
  public void verifyEqualsFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0-SNAPSHOT::ExampleNamedVerticle",
        verifyExample2DeployedOnlyHandler(testContext, "DEFAULT")
    );
  }


  private Handler<AsyncResult<String>> verifyBothDeployedHandler(final TestContext testContext, final String replyMsg) {
    Async async = testContext.async();
    return e -> {
      try {
        if (e.succeeded()) {
          vertx.eventBus().<String>send("example1", null, result -> {
            if (result.failed()) {
              testContext.fail(result.cause());
            }
            testContext.assertEquals("Hello " + replyMsg, result.result().body());
          });

          vertx.eventBus().<String>send("example2", null, result -> {
            if (result.failed()) {
              testContext.fail(result.cause());
            }
            testContext.assertEquals("Hello " + replyMsg, result.result().body());
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

  private Handler<AsyncResult<String>> verifyExample2DeployedOnlyHandler(final TestContext testContext, final String replyMsg) {
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
            testContext.assertEquals("Hello " + replyMsg, result.result().body());
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
