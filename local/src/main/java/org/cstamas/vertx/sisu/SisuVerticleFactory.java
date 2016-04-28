package org.cstamas.vertx.sisu;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.VerticleFactory;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

/**
 * A {@link VerticleFactory} that uses given class loader to create Sisu container and lookup {@link Verticle}
 * by FQ class name or name. Prefix is {@code sisu}. The {@link Verticle} should be annotated with
 * {@link javax.inject.Named} and discoverable by Sisu Index.
 */
public class SisuVerticleFactory
    implements VerticleFactory
{
  private static final Logger log = LoggerFactory.getLogger(SisuVerticleFactory.class);

  public static final String PREFIX = "sisu";

  private HashMap<String, Injector> injectorsByIdentifier;

  private InjectorFactory injectorFactory;

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void init(final Vertx vertx) {
    this.injectorsByIdentifier = new HashMap<>();
    this.injectorFactory = new SimpleInjectorFactory(vertx);
  }

  @Override
  public boolean requiresResolve() {
    return true;
  }

  @Override
  public boolean blockingCreate() {
    return true;
  }

  @Override
  public synchronized void resolve(String identifier,
                                   DeploymentOptions deploymentOptions,
                                   ClassLoader classLoader,
                                   Future<String> resolution)
  {
    log.debug("resolve: " + identifier);

    ArrayList<Module> modules = new ArrayList<>(1);
    modules.add(binder -> binder.bind(DeploymentOptions.class).toInstance(deploymentOptions));
    final String serviceFilter = parseIdentifier(identifier).serviceFilter;
    if (serviceFilter != null) {
      modules.add(binder -> binder.bindConstant().annotatedWith(Names.named("bootstrap.filter")).to(serviceFilter));
    }

    if (!injectorsByIdentifier.containsKey(identifier)) {
      injectorsByIdentifier.put(
          identifier,
          injectorFactory.injectorFor(classLoader, modules)
      );
    }
    else {
      throw new IllegalArgumentException("Verticle " + identifier + " already looked up!");
    }
    resolution.complete(identifier); // return identifier to not re-resolve
  }

  @Override
  public Verticle createVerticle(final String identifier, final ClassLoader classLoader) throws Exception {
    final String verticleName = parseIdentifier(identifier).verticleName;

    //final Injector verticleInjector = injectorsByClassloader.get(classLoader);
    final Injector verticleInjector = injectorsByIdentifier.get(identifier);
    final BeanLocator beanLocator = verticleInjector.getInstance(BeanLocator.class);

    final Class clazz = tryToLoadClass(classLoader, verticleName);
    if (clazz != null) { // try by class
      return lookup(beanLocator, Key.get(clazz));
    }
    else { // try by name
      return lookup(beanLocator, Key.get(Verticle.class, Names.named(verticleName)));
    }
  }

  private static Class<?> tryToLoadClass(final ClassLoader classLoader, final String className) {
    try {
      return classLoader.loadClass(className);
    }
    catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static Verticle lookup(final BeanLocator beanLocator, final Key<Verticle> key) {
    Iterator<? extends BeanEntry<Annotation, Verticle>> iterator = beanLocator.locate(key).iterator();
    if (iterator.hasNext()) {
      return iterator.next().getProvider().get(); // using provider to support non-singleton pattern
    }
    else {
      return null;
    }
  }

  private static Identifier parseIdentifier(String identifier) {
    String identifierNoPrefix = VerticleFactory.removePrefix(identifier);
    String verticleName = identifierNoPrefix;
    String serviceFilter = null;
    int pos = identifierNoPrefix.lastIndexOf("::");
    if (pos != -1) {
      verticleName = identifierNoPrefix.substring(0, pos);
      serviceFilter = identifierNoPrefix.substring(pos + 2);
    }
    return new Identifier(verticleName, serviceFilter);
  }

  private static class Identifier
  {
    private final String verticleName;

    private final String serviceFilter;

    private Identifier(final String verticleName, final String serviceFilter) {
      this.verticleName = verticleName;
      this.serviceFilter = serviceFilter;
    }
  }
}
