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
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bootstrap {@link Verticle} that gets {@link Map} of {@link Verticle}s injected by Sisu, and it simply relays the
 * lifecycle calls to injected instances.
 */
@Named(BootstrapVerticle.NAME)
public class BootstrapVerticle
    extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(BootstrapVerticle.class);

  public static final String NAME = "bootstrap";

  private final Predicate<String> filter;

  private Map<String, Verticle> verticles;

  public BootstrapVerticle(final Predicate<String> filter) {
    this.filter = checkNotNull(filter);
  }

  @Inject
  public void populateVerticles(final Map<String, Verticle> verticleMap) {
    verticles = verticleMap.entrySet().stream()
        .filter(e -> filter.test(e.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    checkArgument(!verticles.isEmpty(), "No Verticle participating in bootstrap!");
    log.debug("Bootstrapping following verticles: " + verticles);
  }

  @Override
  public void init(final Vertx vertx, final Context context) {
    super.init(vertx, context);
    verticles.values().stream().forEach(e -> e.init(vertx, context));
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    log.debug("Starting bootstrap verticle");
    AtomicInteger atomicInteger = new AtomicInteger(verticles.size());
    for (Verticle verticle : verticles.values()) {
      Future<Void> f = Future.future();
      f.setHandler(delegatingHandler(atomicInteger, startFuture));
      verticle.start(f);
    }
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    log.debug("Stopping bootstrap verticle");
    AtomicInteger atomicInteger = new AtomicInteger(verticles.size());
    for (Verticle verticle : verticles.values()) {
      Future<Void> f = Future.future();
      f.setHandler(delegatingHandler(atomicInteger, stopFuture));
      verticle.stop(f);
    }
  }

  private Handler<AsyncResult<Void>> delegatingHandler(final AtomicInteger counter,
                                                       final Future<Void> lifecycleFuture)
  {
    return (AsyncResult<Void> event) -> {
      int val = counter.decrementAndGet();
      if (event.failed()) {
        lifecycleFuture.fail(event.cause());
      }
      else if (val == 0) {
        lifecycleFuture.complete();
      }
    };
  }
}
