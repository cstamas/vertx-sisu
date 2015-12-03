package org.cstamas.vertx.sisu;

import java.util.IdentityHashMap;

import javax.annotation.Nonnull;

import com.google.inject.Injector;

/**
 * {@link Injector} factory that set up default bindings and creates injector.
 */
public class CachingInjectorFactory
    implements InjectorFactory
{
  private final InjectorFactory injectorFactory;

  private final IdentityHashMap<ClassLoader, Injector> injectorCache;

  public CachingInjectorFactory(final InjectorFactory injectorFactory)
  {
    this.injectorFactory = injectorFactory;
    this.injectorCache = new IdentityHashMap<>();
  }

  @Nonnull
  @Override
  public synchronized Injector injectorFor(final ClassLoader classLoader)
  {
    if (!injectorCache.containsKey(classLoader)) {
      injectorCache.put(classLoader, injectorFactory.injectorFor(classLoader));
    }
    return injectorCache.get(classLoader);
  }
}
