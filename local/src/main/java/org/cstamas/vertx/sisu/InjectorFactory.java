package org.cstamas.vertx.sisu;

import java.util.Map;

import com.google.inject.Injector;

/**
 * {@link Injector} factory.
 *
 * @since 1.0
 */
public interface InjectorFactory
{
  Injector injectorFor(final ClassLoader classLoader, final Map<String, String> parameters);
}
