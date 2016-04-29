package org.cstamas.vertx.sisu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static org.cstamas.vertx.sisu.Filters.filterFromString;

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

  @Inject
  public BootstrapVerticle(@Nullable @Named("bootstrap.filter") final String filterString,
                           final Map<String, Verticle> verticleMap)
  {
    this.filter = filterFromString(filterString);
    this.verticles = verticleMap.entrySet().stream()
        .filter(e -> !NAME.equals(e.getKey()) && filter.test(e.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    if (verticles.isEmpty()) {
      log.warn("No verticle participates in bootstrap? (are they discoverable, or filter '" + filter +
          "' filtered out all of them?)");
    }
    else {
      log.debug("Bootstrap verticle(filter='" + filterString + "', verticles=" + verticles.keySet() + ")");
    }
  }

  @Override
  public void init(final Vertx vertx, final Context context) {
    super.init(vertx, context);
    verticles.values().stream().forEach(e -> e.init(vertx, context));
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    log.debug("Starting bootstrap verticle");
    List<Future> futures = new ArrayList<>(verticles.size());
    for (Verticle verticle : verticles.values()) {
      Future<Void> f = Future.future();
      verticle.start(f);
      futures.add(f);
    }
    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      }
      else {
        startFuture.fail(ar.cause());
      }
    });
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    log.debug("Stopping bootstrap verticle");
    List<Future> futures = new ArrayList<>(verticles.size());
    for (Verticle verticle : verticles.values()) {
      Future<Void> f = Future.future();
      verticle.stop(f);
      futures.add(f);
    }
    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        stopFuture.complete();
      }
      else {
        stopFuture.fail(ar.cause());
      }
    });
  }
}
