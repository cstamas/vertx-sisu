package org.cstamas.vertx.sisu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * {@link Injector} factory.
 */
public interface InjectorFactory
{
  /**
   * Creates sisu enabled {@link Injector}. Implementation may cache injectors.
   *
   * @param classLoader the {@link ClassLoader} to create SISU Class space from.
   * @return SISU enabled Guice injector.
   */
  @Nonnull
  Injector injectorFor(ClassLoader classLoader, @Nullable Iterable<Module> modules);

  /**
   * Creates sisu enabled {@link Injector}. Implementation may cache injectors.
   *
   * @return SISU enabled Guice injector.
   */
  @Nonnull
  Injector injectorFor(Iterable<Module> modules);
}
