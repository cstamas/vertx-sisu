package org.cstamas.vertx.sisu;

import com.google.inject.Injector;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;

import static org.cstamas.vertx.sisu.Utilities.filterFromString;
import static org.cstamas.vertx.sisu.Utilities.shareInstance;

/**
 * A {@link VerticleFactory} that uses given class loader to create Sisu container and lookup all {@link Verticle}s
 * from it. Prefix is {@code sisu-local}. The {@link Verticle}s should be annotated with {@code javax.inject.Named} and
 * discoverable by Sisu Index.
 */
public class SisuLocalVerticleFactory
    implements VerticleFactory
{
  public static final String PREFIX = "sisu-local";

  private InjectorFactory injectorFactory;

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void init(final Vertx vertx) {
    this.injectorFactory = shareInstance(
        vertx,
        InjectorFactory.class.getName(), new CachingInjectorFactory(new SimpleInjectorFactory(vertx))
    );
  }

  @Override
  public Verticle createVerticle(final String identifier, final ClassLoader classLoader) throws Exception {
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

    final BootstrapVerticle bootstrap = new BootstrapVerticle(filterFromString(serviceFilter));
    Injector injector = injectorFactory.injectorFor(classLoader);
    injector.getMembersInjector(BootstrapVerticle.class).injectMembers(bootstrap);
    return bootstrap;
  }
}
