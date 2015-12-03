package org.cstamas.vertx.sisu;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Resolver that resolves given coordinate into set of (JAR) files. It may download/cache/whatever to achieve that.
 */
public interface Resolver
{
  /**
   * Resolves passed in coordinates into list of files. Never returns {@code null}.
   */
  @Nonnull
  List<File> resolve(final String coordinates);
}
