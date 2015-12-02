package org.cstamas.vertx.sisu;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.takari.aether.connector.AetherRepositoryConnectorFactory;
import io.takari.aether.localrepo.TakariLocalRepositoryManagerFactory;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

public class AetherResolver
    implements Resolver
{
  public static final String LOCAL_REPO_SYS_PROP = "vertx.maven.localRepo";

  public static final String REMOTE_REPOS_SYS_PROP = "vertx.maven.remoteRepos";

  public static final String HTTP_PROXY_SYS_PROP = "vertx.maven.httpProxy";

  public static final String HTTPS_PROXY_SYS_PROP = "vertx.maven.httpsProxy";

  public static final String REMOTE_SNAPSHOT_POLICY_SYS_PROP = "vertx.maven.remoteSnapshotPolicy";

  private static final String USER_HOME = System.getProperty("user.home");

  private static final String FILE_SEP = System.getProperty("file.separator");

  private static final String DEFAULT_MAVEN_LOCAL = USER_HOME + FILE_SEP + ".m2" + FILE_SEP + "repository";

  private static final String DEFAULT_MAVEN_REMOTES =
      "https://repo.maven.apache.org/maven2/ https://oss.sonatype.org/content/repositories/snapshots/";

  private final String localMavenRepo;

  private final List<String> remoteMavenRepos;

  private final String httpProxy;

  private final String httpsProxy;

  private final RepositorySystem repositorySystem;

  public AetherResolver() {
    localMavenRepo = System.getProperty(LOCAL_REPO_SYS_PROP, DEFAULT_MAVEN_LOCAL);
    String remoteString = System.getProperty(REMOTE_REPOS_SYS_PROP, DEFAULT_MAVEN_REMOTES);
    // They are space delimited (space is illegal char in urls)
    remoteMavenRepos = Arrays.asList(remoteString.split(" "));
    httpProxy = System.getProperty(HTTP_PROXY_SYS_PROP);
    httpsProxy = System.getProperty(HTTPS_PROXY_SYS_PROP);

    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, AetherRepositoryConnectorFactory.class);
    locator.addService(LocalRepositoryManagerFactory.class, TakariLocalRepositoryManagerFactory.class);
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler()
    {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        exception.printStackTrace();
      }
    });
    this.repositorySystem = locator.getService(RepositorySystem.class);
  }

  private static Authentication extractAuth(final URL url) {
    String userInfo = url.getUserInfo();
    if (userInfo != null) {
      AuthenticationBuilder authBuilder = new AuthenticationBuilder();
      int sep = userInfo.indexOf(':');
      if (sep != -1) {
        authBuilder.addUsername(userInfo.substring(0, sep));
        authBuilder.addPassword(userInfo.substring(sep + 1));
      }
      else {
        authBuilder.addUsername(userInfo);
      }
      return authBuilder.build();
    }
    return null;
  }

  private static void customizeRemoteRepoBuilder(final RemoteRepository.Builder builder) {
    String updatePolicy = System.getProperty(REMOTE_SNAPSHOT_POLICY_SYS_PROP);
    if (updatePolicy != null && !updatePolicy.isEmpty()) {
      builder.setSnapshotPolicy(new RepositoryPolicy(true, updatePolicy, RepositoryPolicy.CHECKSUM_POLICY_FAIL));
    }
  }

  @Override
  public List<File> resolve(final String coordinates) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    try {
      Proxy proxy = null;
      if (httpProxy != null) {
        URL url = new URL(httpProxy);
        Authentication authentication = extractAuth(url);
        proxy = new Proxy("http", url.getHost(), url.getPort(), authentication);
      }
      Proxy secureProxy = null;
      if (httpsProxy != null) {
        URL url = new URL(httpsProxy);
        Authentication authentication = extractAuth(url);
        secureProxy = new Proxy("https", url.getHost(), url.getPort(), authentication);
      }

      LocalRepository localRepo = new LocalRepository(localMavenRepo);
      session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));

      int count = 0;
      List<RemoteRepository> remotes = new ArrayList<>();
      for (String remote : remoteMavenRepos) {
        URL url = new URL(remote);
        Authentication auth = extractAuth(url);
        if (auth != null) {
          url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
        }
        RemoteRepository.Builder builder = new RemoteRepository.Builder("repo" + (count++), "default",
            url.toString());
        if (auth != null) {
          builder.setAuthentication(auth);
        }
        switch (url.getProtocol()) {
          case "http":
            if (proxy != null) {
              builder.setProxy(proxy);
            }
            break;
          case "https":
            if (secureProxy != null) {
              builder.setProxy(secureProxy);
            }
            break;
        }
        customizeRemoteRepoBuilder(builder);
        RemoteRepository remoteRepo = builder.build();
        remotes.add(remoteRepo);
      }

      DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
      CollectRequest collectRequest = new CollectRequest();
      collectRequest.setRoot(new Dependency(new DefaultArtifact(coordinates), JavaScopes.COMPILE));
      collectRequest.setRepositories(remotes);

      DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFilter);
      try {
        return repositorySystem.resolveDependencies(session, dependencyRequest).getArtifactResults().stream().
            map(res -> res.getArtifact().getFile().getAbsoluteFile()).
            collect(Collectors.toList());
      }
      catch (DependencyResolutionException e) {
        throw new IllegalArgumentException("Cannot resolve module " + coordinates +
            " in maven repositories: " + e.getMessage());
      }
      catch (NullPointerException e) {
        // Sucks, but aether throws a NPE if repository name is invalid....
        throw new IllegalArgumentException(
            "Cannot find module " + coordinates + ". Maybe repository URL is invalid?");
      }
    }
    catch (MalformedURLException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }
}
