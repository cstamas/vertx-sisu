package org.cstamas.vertx.sisu;

import javax.annotation.Nullable;

import io.vertx.core.spi.VerticleFactory;

/**
 * Identifier, that parses and hold the parts of service identifier without prefix.
 */
public final class Identifier
{
  private final String verticleName;

  private final String serviceFilter;

  private Identifier(final String verticleName, @Nullable final String serviceFilter) {
    if (verticleName == null || verticleName.trim().length() == 0) {
      throw new IllegalArgumentException("Verticle name must be non-null non-empty string");
    }
    this.verticleName = verticleName;
    this.serviceFilter = serviceFilter;
  }

  public String getVerticleName() {
    return verticleName;
  }

  @Nullable
  public String getServiceFilter() {
    return serviceFilter;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Identifier that = (Identifier) o;
    if (!verticleName.equals(that.verticleName)) {
      return false;
    }
    return serviceFilter != null ? serviceFilter.equals(that.serviceFilter) : that.serviceFilter == null;
  }

  @Override
  public int hashCode() {
    int result = verticleName.hashCode();
    result = 31 * result + (serviceFilter != null ? serviceFilter.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    if (serviceFilter != null) {
      return verticleName + "::" + serviceFilter;
    }
    else {
      return verticleName;
    }
  }

  public static Identifier parseIdentifier(String identifier) {
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
}
