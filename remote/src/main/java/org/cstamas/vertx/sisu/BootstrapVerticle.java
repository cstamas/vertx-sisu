package org.cstamas.vertx.sisu;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class BootstrapVerticle
    extends AbstractVerticle
{
  private final Predicate<String> filter;

  private Map<String, Verticle> verticles;

  public BootstrapVerticle(final Predicate<String> filter) {
    this.filter = checkNotNull(filter);
  }

  @Inject
  public void populateVerticles(final Map<String, Verticle> verticleMap) {
    this.verticles = verticleMap.entrySet().stream()
        .filter(e -> filter.test(e.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public void init(final Vertx vertx, final Context context) {
    super.init(vertx, context);
    verticles.values().stream().forEach(e -> e.init(vertx, context));
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    AtomicInteger atomicInteger = new AtomicInteger(verticles.size());
    for (Verticle verticle : verticles.values()) {
      Future<Void> f = Future.future();
      f.setHandler((AsyncResult<Void> event) -> {
        int val = atomicInteger.decrementAndGet();
        if (event.failed()) {
          startFuture.fail(event.cause());
        }
        else if (val == 0) {
          startFuture.complete();
        }
      });
      verticle.start(f);
    }
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    AtomicInteger atomicInteger = new AtomicInteger(verticles.size());
    for (Verticle verticle : verticles.values()) {
      Future<Void> f = Future.future();
      f.setHandler((AsyncResult<Void> event) -> {
        int val = atomicInteger.decrementAndGet();
        if (event.failed()) {
          stopFuture.fail(event.cause());
        }
        else if (val == 0) {
          stopFuture.complete();
        }
      });
      verticle.stop(f);
    }
  }
}
