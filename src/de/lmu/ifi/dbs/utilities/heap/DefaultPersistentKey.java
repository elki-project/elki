package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.utilities.Util;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultPersistentKey implements PersistentKey {
  private double key;

  public DefaultPersistentKey(double key) {
    this.key = key;
  }

  /**
   * Returns the size of this key in Bytes.
   * @return the size of this key in Bytes
   */
  public int size() {
    return 8;
  }

  /**
   * Compares this PersistentKey with the specified PersistentKey.
   *
   * @param key PersistentKey to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(PersistentKey key) {
    DefaultPersistentKey other = (DefaultPersistentKey) key;
    if (this.key < other.key) return -1;
    if (this.key > other.key) return +1;
    return 0;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return Util.format(key);
  }


}
