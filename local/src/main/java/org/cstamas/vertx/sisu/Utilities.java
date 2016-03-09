package org.cstamas.vertx.sisu;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vertx.core.Vertx;

import static java.util.Objects.requireNonNull;

/**
 * Utilities.
 */
public final class Utilities
{
  private Utilities() {
    // nop
  }

  private static final ConcurrentMap<Integer, ConcurrentMap<String, Object>> shared = new ConcurrentHashMap<>();

  /**
   * Simple "shared data" per vertx instance to be used by verticle factories, given that Vert.x "shared data" is not
   * ready yet, as factories are registered from vertx implementation ctor, and deployment manager is invoked before
   * shared data instance is created.
   *
   * @param <T>      the type of the shared instance.
   * @param vertx    The vertx instance that is the scope of sharing.
   * @param name     The instance name (key).
   * @param instance the instance to share.
   * @return if given vertx scope and name already has bounded instance, that one, or the newly shared (just passed in)
   * instance.
   */
  @Nonnull
  public static <T> T shareInstance(final Vertx vertx, final String name, final T instance) {
    requireNonNull(vertx);
    requireNonNull(name);
    requireNonNull(instance);
    ConcurrentMap<String, Object> vertxShared = new ConcurrentHashMap<>();
    ConcurrentMap<String, Object> existingVertxShared = shared.putIfAbsent(
        System.identityHashCode(vertx),
        vertxShared
    );
    if (existingVertxShared != null) {
      vertxShared = existingVertxShared;
    }
    T existing = (T) vertxShared.putIfAbsent(name, instance);
    if (existing != null) {
      return existing;
    }
    else {
      return instance;
    }
  }

  /**
   * Creates a {#link Predicate} for filtering sisu bean names.
   *
   * @param filterStr the filter expression as string.
   * @return filtering predicate.
   */
  @Nonnull
  public static Predicate<String> filterFromString(@Nullable final String filterStr) {
    if (filterStr == null) {
      return (String input) -> true;
    }
    else if (filterStr.startsWith("*")) {
      return (String input) -> input.endsWith(filterStr.substring(1));
    }
    else if (filterStr.endsWith("*")) {
      return (String input) -> input.startsWith(filterStr.substring(0, filterStr.length() - 1));
    }
    else {
      return (String input) -> input.equals(filterStr);
    }
  }
}
