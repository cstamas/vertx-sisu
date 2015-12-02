package org.cstamas.vertx.sisu;

import java.io.File;
import java.util.List;

public interface Resolver
{
  List<File> resolve(final String coordinates);
}
