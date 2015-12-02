package org.cstamas.vertx.sisu;

import java.util.Map;

import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * {@link Injector} factory.
 *
 * @since 1.0
 */
public interface InjectorFactory
{
  Injector injectorFor(final ClassLoader classLoader, final Map<String, String> parameters, final Module... modules);
}
