package org.cstamas.vertx.sisu;

import io.vertx.core.Vertx;
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
        "sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT",
        e -> {
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
    );
  }

  @Test
  public void verifyEndsWithFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT::*NamedVerticle",
        e -> {
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
    );
  }

  @Test
  public void verifyStartsWithFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT::ExampleNamed*",
        e -> {
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
    );
  }

  @Test
  public void verifyEqualsFilter(TestContext testContext) {
    vertx.deployVerticle(
        "sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT::ExampleNamedVerticle",
        e -> {
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
    );
  }
}
