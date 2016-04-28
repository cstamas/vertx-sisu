package org.cstamas.vertx.sisu.examples;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.vertx.core.DeploymentOptions;

@Named
@Singleton
public class MyComponent
{
  private final DeploymentOptions deploymentOptions;

  @Inject
  public MyComponent(final DeploymentOptions deploymentOptions)
  {
    this.deploymentOptions = deploymentOptions;
  }

  public String getReply() {
    return "Hello " + getReplyString();
  }

  public static final String DEFAULT_REPLY = "DEFAULT";

  private String getReplyString() {
    if (deploymentOptions.getConfig() != null) {
      return deploymentOptions.getConfig().getString("reply.test", DEFAULT_REPLY);
    }
    return DEFAULT_REPLY;
  }
}
