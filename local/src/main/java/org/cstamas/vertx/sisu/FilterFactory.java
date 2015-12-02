package org.cstamas.vertx.sisu;

import java.util.function.Predicate;

public interface FilterFactory
{
  Predicate<String> filter(final String filterString);
}
