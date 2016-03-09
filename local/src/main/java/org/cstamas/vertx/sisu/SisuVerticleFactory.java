package org.cstamas.vertx.sisu;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import com.google.inject.Key;
import com.google.inject.name.Names;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.verticle.CompilingClassLoader;
import io.vertx.core.spi.VerticleFactory;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

import static org.cstamas.vertx.sisu.Utilities.shareInstance;

/**
 * A {@link VerticleFactory} that uses given class loader to create Sisu container and lookup {@link Verticle}
 * by FQ class name or name. Prefix is {@code sisu}. The {@link Verticle} should be annotated with
 * {@link javax.inject.Named} and discoverable by Sisu Index.
 */
public class SisuVerticleFactory
    implements VerticleFactory
{
  public static final String PREFIX = "sisu";

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

    final BeanLocator beanLocator = injectorFactory.injectorFor(classLoader).getInstance(BeanLocator.class);
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
