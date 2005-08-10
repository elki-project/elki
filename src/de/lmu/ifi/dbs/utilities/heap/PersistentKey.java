package de.lmu.ifi.dbs.utilities.heap;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface PersistentKey extends Comparable<PersistentKey> {
  /**
   * Returns the size of this key in Bytes.
   * @return the size of this key in Bytes
   */
  int size();
}
