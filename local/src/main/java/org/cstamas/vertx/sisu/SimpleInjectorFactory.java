package org.cstamas.vertx.sisu;

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.vertx.core.Vertx;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;

/**
 * {@link Injector} factory that set up default bindings.
 *
 * @since 1.0
 */
public class SimpleInjectorFactory
    implements InjectorFactory
{
  private final Vertx vertx;

  public SimpleInjectorFactory(final Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Injector injectorFor(final ClassLoader classLoader, final Map<String, String> parameters) {
    return Guice.createInjector(
        Stage.DEVELOPMENT,
        new WireModule(
            new AbstractModule()
            {
              @Override
              protected void configure() {
                bind(Vertx.class).toInstance(vertx);
                if (parameters != null) {
                  bind(ParameterKeys.PROPERTIES).toInstance(parameters);
                }
              }
            },
            new SpaceModule(
                new URLClassSpace(classLoader), BeanScanning.INDEX
            )
        )
    );
  }
}
