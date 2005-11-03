package de.lmu.ifi.dbs.utilities;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface KListEntry<T> extends Comparable<KListEntry<T>> {
  /**
   * Returns the key of this entry.
   * @return the key of this entry
   */
  T getKey();
}
