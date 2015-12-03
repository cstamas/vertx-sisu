package org.cstamas.vertx.sisu;

import javax.annotation.Nonnull;

import com.google.inject.Injector;

/**
 * {@link Injector} factory.
 */
public interface InjectorFactory
{
  /**
   * Creates sisu enabled {@link Injector}. Implementation may cache injectors.
   */
  @Nonnull
  Injector injectorFor(final ClassLoader classLoader);
}
