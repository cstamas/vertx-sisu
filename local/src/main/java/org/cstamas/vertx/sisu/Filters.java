package org.cstamas.vertx.sisu;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Filters.
 */
public final class Filters
{
  private Filters() {
    // nop
  }

  /**
   * Creates a {#link Predicate} for filtering sisu bean names.
   *
   * @param filterStr the filter expression as string.
   * @return filtering predicate.
   */
  @Nonnull
  public static Predicate<String> filterFromString(@Nullable final String filterStr) {
    if (filterStr == null || filterStr.trim().length() == 0) {
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
