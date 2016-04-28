package org.cstamas.vertx.sisu;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
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
  public static final String PREFIX = "sisu";

  private IdentityHashMap<ClassLoader, Injector> injectorsByClassloader;

  private InjectorFactory injectorFactory;

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void init(final Vertx vertx) {
    this.injectorsByClassloader = new IdentityHashMap<>();
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
    ArrayList<Module> modules = new ArrayList<>(1);
    String identifierNoPrefix = VerticleFactory.removePrefix(identifier);
    String verticleName = identifierNoPrefix;
    int pos = identifierNoPrefix.lastIndexOf("::");
    if (pos != -1) {
      verticleName = identifierNoPrefix.substring(0, pos);
      String serviceFilter = identifierNoPrefix.substring(pos + 2);
      modules.add(new AbstractModule()
      {
        @Override
        protected void configure() {
          bindConstant().annotatedWith(Names.named("bootstrap.filter")).to(serviceFilter);
        }
      });
    }

    modules.add(binder ->
        binder.bind(DeploymentOptions.class).toInstance(deploymentOptions));

    if (!injectorsByClassloader.containsKey(classLoader)) {
      injectorsByClassloader.put(
          classLoader,
          injectorFactory.injectorFor(
              classLoader,
              modules.toArray(new Module[modules.size()])
          )
      );
    }
    resolution.complete(PREFIX + ":" + verticleName);
  }

  @Override
  public Verticle createVerticle(final String identifier, final ClassLoader classLoader) throws Exception {
    String verticleName = VerticleFactory.removePrefix(identifier);

    final Injector verticleInjector = injectorsByClassloader.get(classLoader);
    final BeanLocator beanLocator = verticleInjector.getInstance(BeanLocator.class);

    final Class clazz = tryToLoadClass(classLoader, verticleName);
    if (clazz != null) { // try by class
      return lookup(beanLocator, Key.get(clazz));
    }
    else { // try by name
      return lookup(beanLocator, Key.get(Verticle.class, Names.named(verticleName)));
    }
  }

  private Class<?> tryToLoadClass(final ClassLoader classLoader, final String className) {
    try {
      return classLoader.loadClass(className);
    }
    catch (ClassNotFoundException e) {
      return null;
    }
  }

  private Verticle lookup(final BeanLocator beanLocator, final Key<Verticle> key) {
    Iterator<? extends BeanEntry<Annotation, Verticle>> iterator = beanLocator.locate(key).iterator();
    if (iterator.hasNext()) {
      return iterator.next().getProvider().get(); // using provider to support non-singleton pattern
    }
    else {
      return null;
    }
  }
}
