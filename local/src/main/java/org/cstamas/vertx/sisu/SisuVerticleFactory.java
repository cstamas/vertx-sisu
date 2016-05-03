package org.cstamas.vertx.sisu;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.eclipse.sisu.inject.DefaultRankingFunction;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.inject.RankingFunction;

import static org.cstamas.vertx.sisu.Identifier.parseIdentifier;

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

  private IdentityHashMap<ClassLoader, Injector> injectorByClassloader;

  private HashMap<Identifier, Injector> injectorsByIdentifier;

  private InjectorFactory injectorFactory;

  private final AtomicInteger childModuleRank = new AtomicInteger(1);

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public void init(final Vertx vertx) {
    this.injectorByClassloader = new IdentityHashMap<>();
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
  public synchronized void resolve(String identifierStr,
                                   DeploymentOptions deploymentOptions,
                                   ClassLoader classLoader,
                                   Future<String> resolution)
  {
    log.debug("resolve: " + identifierStr);
    final Identifier identifier = parseIdentifier(identifierStr);

    if (!injectorByClassloader.containsKey(classLoader)) {
      injectorByClassloader.put(classLoader, injectorFactory.injectorFor(classLoader, null));
    }
    final Injector parent = injectorByClassloader.get(classLoader);

    ArrayList<Module> modules = new ArrayList<>(1);

    modules.add(binder -> {
      binder.bind(MutableBeanLocator.class).toInstance(parent.getInstance(MutableBeanLocator.class));
      binder.bind(RankingFunction.class).toInstance(new DefaultRankingFunction(childModuleRank.incrementAndGet()));

      binder.bind(DeploymentOptions.class).toInstance(deploymentOptions);
      // binder.bind(Verticle.class).annotatedWith(Names.named(BootstrapVerticle.NAME)).to(BootstrapVerticle.class);
      if (identifier.getServiceFilter() != null) {
        binder.bindConstant().annotatedWith(Names.named("bootstrap.filter")).to(identifier.getServiceFilter());
      }
    });
    if (!injectorsByIdentifier.containsKey(identifier)) {
      injectorsByIdentifier.put(identifier, injectorFactory.injectorFor(modules));
    }
    else {
      throw new IllegalArgumentException("Verticle " + identifier + " already looked up!");
    }
    resolution.complete(identifierStr); // return same identifier to not re-resolve
  }

  @Override
  public Verticle createVerticle(final String identifierStr, final ClassLoader classLoader) throws Exception {
    final Identifier identifier = parseIdentifier(identifierStr);
    final Injector verticleInjector = injectorsByIdentifier.get(identifier);
    final BeanLocator beanLocator = verticleInjector.getInstance(BeanLocator.class);
    final Class clazz = tryToLoadClass(classLoader, identifier.getVerticleName());
    if (clazz != null) { // try by class
      return lookup(beanLocator, Key.get(clazz));
    }
    else { // try by name
      return lookup(beanLocator, Key.get(Verticle.class, Names.named(identifier.getVerticleName())));
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
}
