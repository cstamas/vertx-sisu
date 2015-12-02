package org.cstamas.vertx.sisu;

import java.lang.annotation.Annotation;
import java.util.IdentityHashMap;
import java.util.Iterator;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.verticle.CompilingClassLoader;
import io.vertx.core.spi.VerticleFactory;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

/**
 * A {@link VerticleFactory} that uses given class loader to create Sisu container and lookup {@link Verticle}
 * by FQ class name or name. Prefix is {@code sisu}. The {@link Verticle} should be annotated with {@link @Named} and
 * discoverable by Sisu.
 *
 * @since 1.0
 */
public class SisuVerticleFactory
    implements VerticleFactory
{
  public static final String PREFIX = "sisu";

  private InjectorFactory injectorFactory;

  private IdentityHashMap<ClassLoader, Injector> injectorCache;

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void init(final Vertx vertx) {
    this.injectorFactory = new SimpleInjectorFactory(vertx);
    this.injectorCache = new IdentityHashMap<>();
  }

  @Override
  public Verticle createVerticle(final String identifier, final ClassLoader classLoader) throws Exception {
    if (!injectorCache.containsKey(classLoader)) {
      injectorCache.put(classLoader, injectorFactory.injectorFor(classLoader, null));
    }
    final BeanLocator beanLocator = injectorCache.get(classLoader).getInstance(BeanLocator.class);

    String verticleName = VerticleFactory.removePrefix(identifier);
    Class clazz;
    if (verticleName.endsWith(".java")) {
      CompilingClassLoader compilingLoader = new CompilingClassLoader(classLoader, verticleName);
      String className = compilingLoader.resolveMainClassName();
      clazz = tryToLoadClass(compilingLoader, className);
    }
    else {
      clazz = tryToLoadClass(classLoader, verticleName);
    }

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
