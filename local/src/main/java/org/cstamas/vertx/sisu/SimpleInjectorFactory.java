package org.cstamas.vertx.sisu;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;

import static java.util.Objects.requireNonNull;

/**
 * {@link Injector} factory that set up default bindings and creates injector.
 */
public class SimpleInjectorFactory
    implements InjectorFactory
{
  private final Vertx vertx;

  private final Map<String, String> parameters;

  private final List<Module> bootstrapModules;

  public SimpleInjectorFactory(final Vertx vertx) {
    this(vertx, null, null);
  }

  public SimpleInjectorFactory(final Vertx vertx,
                               @Nullable final Map<String, String> parameters,
                               @Nullable final List<Module> bootstrapModules)
  {
    this.vertx = requireNonNull(vertx);
    this.parameters = parameters;
    this.bootstrapModules = bootstrapModules;
  }

  @Override
  public Injector injectorFor(final ClassLoader classLoader, @Nullable final Iterable<Module> modules)
  {
    return Guice.createInjector(
        Stage.DEVELOPMENT,
        new WireModule(
            new AbstractModule() // params
            {
              @Override
              protected void configure() {
                bind(Vertx.class).toInstance(vertx);
                bind(EventBus.class).toInstance(vertx.eventBus());
                if (parameters != null) {
                  bind(ParameterKeys.PROPERTIES).toInstance(parameters);
                }
                if (bootstrapModules != null) {
                  for (Module bootstrapModule : bootstrapModules) {
                    install(bootstrapModule);
                  }
                }
                if (modules != null) {
                  for (Module module : modules) {
                    install(module);
                  }
                }
              }
            },
            new SpaceModule( // space module
                new URLClassSpace(classLoader), BeanScanning.INDEX
            )
        )
    );
  }

  @Override
  public Injector injectorFor(final Iterable<Module> modules)
  {
    return Guice.createInjector(
        Stage.DEVELOPMENT,
        new AbstractModule() // params
        {
          @Override
          protected void configure() {
            for (Module module : modules) {
              install(module);
            }
          }
        }
    );
  }
}
