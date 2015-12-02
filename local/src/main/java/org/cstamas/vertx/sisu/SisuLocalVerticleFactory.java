package org.cstamas.vertx.sisu;

import java.util.IdentityHashMap;

import com.google.inject.Injector;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.eclipse.sisu.inject.BeanLocator;

/**
 * A {@link VerticleFactory} that uses given class loader to create Sisu container and lookup all {@link Verticle}s
 * from it. Prefix is {@code sisu-local}. The {@link Verticle}s should be annotated with {@link @Named} and
 * discoverable by Sisu.
 *
 * @since 1.0
 */
public class SisuLocalVerticleFactory
    implements VerticleFactory
{
  public static final String PREFIX = "sisu-local";

  private InjectorFactory injectorFactory;

  private FilterFactory filterFactory;

  private IdentityHashMap<ClassLoader, Injector> injectorCache;

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void init(final Vertx vertx) {
    this.injectorFactory = new SimpleInjectorFactory(vertx);
    this.filterFactory = new SimpleFilterFactory();
    this.injectorCache = new IdentityHashMap<>();
  }

  @Override
  public Verticle createVerticle(final String identifier, final ClassLoader classLoader) throws Exception {
    if (!injectorCache.containsKey(classLoader)) {
      injectorCache.put(classLoader, injectorFactory.injectorFor(classLoader, null));
    }
    String identifierNoPrefix = VerticleFactory.removePrefix(identifier);
    String name = identifierNoPrefix;
    String serviceFilter = null;
    int pos = identifierNoPrefix.lastIndexOf("::");
    if (pos != -1) {
      name = identifierNoPrefix.substring(0, pos);
      serviceFilter = identifierNoPrefix.substring(pos + 2);
    }
    if (!BootstrapVerticle.NAME.equals(name)) {
      throw new IllegalArgumentException("Unexpected identifier: " + identifier);
    }

    final BootstrapVerticle bootstrap = new BootstrapVerticle(filterFactory.filter(serviceFilter));
    Injector injector = injectorCache.get(classLoader);
    injector.getMembersInjector(BootstrapVerticle.class).injectMembers(bootstrap);
    return bootstrap;
  }
}
