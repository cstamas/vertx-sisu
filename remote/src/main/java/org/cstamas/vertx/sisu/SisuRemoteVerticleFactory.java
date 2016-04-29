package org.cstamas.vertx.sisu;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;

import static org.cstamas.vertx.sisu.Identifier.parseIdentifier;

/**
 * A {@link VerticleFactory} that uses given coordinate to download remote artifact (with dependencies), and then
 * delegate verticle creation to {@link SisuVerticleFactory} to lookup all {@link Verticle}s from it. Prefix is
 * {@code sisu-remote}.
 */
public class SisuRemoteVerticleFactory
    implements VerticleFactory
{
  public static final String PREFIX = "sisu-remote";

  private Vertx vertx;

  private Resolver resolver;

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
    this.resolver = new AetherResolver();
  }

  @Override
  public String prefix() {
    return PREFIX;
  }

  @Override
  public boolean requiresResolve() {
    return true;
  }

  @Override
  public void resolve(
      final String identifierStr,
      final DeploymentOptions deploymentOptions,
      final ClassLoader classLoader,
      final Future<String> resolution)
  {
    vertx.<Void>executeBlocking(fut -> {
      try {
        final Identifier identifier = parseIdentifier(identifierStr);

        List<File> artifacts = resolver.resolve(identifier.getVerticleName());

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
        deploymentOptions.setIsolationGroup("__vertx_sisu_" + identifier.getVerticleName());
        if (identifier.getServiceFilter() != null) {
          resolution.complete(SisuVerticleFactory.PREFIX
              + ":" + BootstrapVerticle.NAME
              + "::" + identifier.getServiceFilter());
        }
        else {
          resolution.complete(SisuVerticleFactory.PREFIX
              + ":" + BootstrapVerticle.NAME);
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
    throw new IllegalStateException("This method should not be invoked");
  }
}
