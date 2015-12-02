package org.cstamas.vertx.sisu;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.inject.Injector;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.eclipse.sisu.inject.BeanLocator;

/**
 * A {@link VerticleFactory} that uses Eclipse Aether to resolve artifact with {@link Verticle} from a Maven repository
 * along with it's all transitive dependencies, and then use Eclipse SISU to create {@link Verticle} instances.
 * Prefix is {@code sisu-remote}.
 *
 * {@code sisu-remote:groupId:artifactId:version}
 *
 * @since 1.0
 */
public class SisuRemoteVerticleFactory
    implements VerticleFactory
{
  private static final String BOOSTRAP_NAME = "bootstrap";

  private Vertx vertx;

  private Resolver resolver;

  private InjectorFactory injectorFactory;

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
    this.injectorFactory = new InjectorFactory(vertx);
    this.resolver = new AetherResolver();
  }

  @Override
  public String prefix() {
    return "sisu-remote";
  }

  @Override
  public boolean requiresResolve() {
    return true;
  }

  @Override
  public void resolve(
      final String identifier,
      final DeploymentOptions deploymentOptions,
      final ClassLoader classLoader,
      final Future<String> resolution)
  {
    if (identifier.startsWith(prefix() + ":" + BOOSTRAP_NAME)) {
      resolution.complete(identifier);
      return;
    }
    vertx.<Void>executeBlocking(fut -> {
      try {
        String identifierNoPrefix = VerticleFactory.removePrefix(identifier);
        String coordsString = identifierNoPrefix;
        String serviceFilter = null;
        int pos = identifierNoPrefix.lastIndexOf("::");
        if (pos != -1) {
          coordsString = identifierNoPrefix.substring(0, pos);
          serviceFilter = identifierNoPrefix.substring(pos + 2);
        }

        List<File> artifacts = resolver.resolve(coordsString);

        // Generate the classpath - if the jar is already on the Vert.x classpath (e.g. the Vert.x dependencies, netty etc)
        // then we don't add it to the classpath for the module
        List<String> classpath = artifacts.stream().
            map(File::getAbsolutePath).collect(Collectors.toList());
        URL[] urls = new URL[classpath.size()];
        int index = 0;
        List<String> extraCP = new ArrayList<>(urls.length);
        for (String pathElement : classpath) {
          File file = new File(pathElement);
          extraCP.add(file.getAbsolutePath());
          try {
            URL url = file.toURI().toURL();
            urls[index++] = url;
          }
          catch (MalformedURLException e) {
            throw new IllegalStateException(e);
          }
        }

        deploymentOptions.setExtraClasspath(extraCP);
        deploymentOptions.setIsolationGroup("__vertx_sisu_" + coordsString);
        if (serviceFilter != null) {
          resolution.complete(prefix() + ":" + BOOSTRAP_NAME + "::" + serviceFilter);
        }
        else {
          resolution.complete(prefix() + ":" + BOOSTRAP_NAME);
        }
      }
      catch (Exception e) {
        fut.fail(e);
        resolution.fail(e);
      }
    }, ar -> {
    });
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
    if (!BOOSTRAP_NAME.equals(name)) {
      throw new IllegalArgumentException("Unexpected identifier: " + identifier);
    }

    final BootstrapVerticle bootstrap = new BootstrapVerticle(filter(serviceFilter));
    Injector injector = injectorFactory.injectorFor(classLoader, null);
    injector.getMembersInjector(BootstrapVerticle.class).injectMembers(bootstrap);
    return bootstrap;
  }

  private static Predicate<String> filter(final String filterStr) {
    if (filterStr == null) {
      return (String input) -> true;
    }
    else if (filterStr.startsWith("*")) {
      return (String input) -> input.endsWith(filterStr.substring(1));
    }
    else if (filterStr.endsWith("*")) {
      return (String input) -> input.startsWith(filterStr.substring(0, filterStr.length() - 1));
    }
    else {
      return (String input) -> input.equals(filterStr);
    }
  }
}
