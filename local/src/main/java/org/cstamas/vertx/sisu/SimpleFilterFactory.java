package org.cstamas.vertx.sisu;

import java.util.function.Predicate;

public class SimpleFilterFactory
    implements FilterFactory
{
  @Override
  public Predicate<String> filter(final String filterStr) {
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
