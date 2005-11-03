package de.lmu.ifi.dbs.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A wrapper class for storing the k most similar comparable objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KList<K, V extends KListEntry<K>> {
  /**
   * The underlying set.
   */
  private SortedSet<V> list;

  /**
   * The maximum size of this list.
   */
  private int k;

  /**
   * The infinite key.
   */
  private K infiniteKey;

  /**
   * Creates a new KList with the specified parameters.
   *
   * @param k           the number k of objects to be stored
   * @param infiniteKey the infinite key
   */
  public KList(int k, K infiniteKey) {
    this.list = new TreeSet<V>();
    this.k = k;
    this.infiniteKey = infiniteKey;
  }

  /**
   * Adds a new object to this list. If this list contains already
   * k entries and the key of the specified object o is less than
   * the key of the last entry, the last entry will be deleted.
   *
   * @param o the object to be added
   * @return true, if o has been added, false otherwise.
   */
  public boolean add(V o) {
    if (list.size() < k) {
      list.add(o);
      return true;
    }

    V last = list.last();

    if (o.compareTo(last) < 0) {
      list.remove(last);
      list.add(o);
      return true;
    }

    return false;
  }

  /**
   * Returns the maximum key of this list (e.g. the key
   * of the last element). If this list is empty an infinite key will
   * be returned.
   *
   * @return the maximum key of this list
   */
  public K getMaximumKey() {
    if (list.isEmpty())
      return infiniteKey;

    KListEntry<K> last = list.last();
    return last.getKey();
  }

  /**
   * Returns a list representation of this KList.
   *
   * @return a list representation of this KList
   */
  public List<V> toList() {
    return new ArrayList<V>(list);
  }

  /**
   * Returns the current size of this list.
   *
   * @return the current size of this list
   */
  public int size() {
    return list.size();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return list + " , max-key = " + getMaximumKey();
  }
}
