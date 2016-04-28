package org.cstamas.vertx.sisu;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.cstamas.vertx.sisu.examples.ExampleNamedVerticle;
import org.cstamas.vertx.sisu.examples.ExampleVerticle;
import org.cstamas.vertx.sisu.examples.MyComponent;
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
  public void bootstrapWithoutConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + BootstrapVerticle.NAME,
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, MyComponent.DEFAULT_REPLY)
    );
  }

  @Test
  public void bootstrapWithConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + BootstrapVerticle.NAME,
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "bootstrapWithConfig")),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, "bootstrapWithConfig")
    );
  }

  @Test
  public void bootstrapWithFilterWithoutConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + BootstrapVerticle.NAME + "::Example*",
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, MyComponent.DEFAULT_REPLY)
    );
  }

  @Test
  public void bootstrapWithFilterWithConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + BootstrapVerticle.NAME + "::*Verticle",
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "bootstrapWithFilterWithConfig")),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, "bootstrapWithFilterWithConfig")
    );
  }


  @Test
  public void byClassWithoutConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.class.getName(),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, MyComponent.DEFAULT_REPLY)
    );
  }

  @Test
  public void byClassWithConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.class.getName(),
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "byClassWithConfig")),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, "byClassWithConfig")
    );
  }

  @Test
  public void byNameWithoutConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.NAME,
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, MyComponent.DEFAULT_REPLY)
    );
  }

  @Test
  public void byNameWithConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.NAME,
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "byNameWithConfig")),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, "byNameWithConfig")
    );
  }

  @Test
  public void twoByClassWithConfig(TestContext testContext) throws Exception {
    vertx.deployVerticle(
        "sisu:" + ExampleNamedVerticle.class.getName(),
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "ExampleNamedVerticle")),
        verifyAddressIsAliveHandler(testContext, ExampleNamedVerticle.ADDR, "ExampleNamedVerticle")
    );
    vertx.deployVerticle(
        "sisu:" + ExampleVerticle.class.getName(),
        new DeploymentOptions().setConfig(new JsonObject().put("reply.test", "ExampleVerticle")),
        verifyAddressIsAliveHandler(testContext, ExampleVerticle.ADDR, "ExampleVerticle")
    );
  }


  private Handler<AsyncResult<String>> verifyAddressIsAliveHandler(final TestContext testContext,
                                                                   final String address,
                                                                   final String replyMsg)
  {
    Async async = testContext.async();
    return result -> {
      try {
        if (result.succeeded()) {
          vertx.eventBus().<String>send(address, "message", new DeliveryOptions().setSendTimeout(1000), r -> {
            if (r.failed()) {
              testContext.fail(r.cause());
            }
            else {
              testContext.assertEquals("Hello " + replyMsg, r.result().body());
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
