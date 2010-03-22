package de.lmu.ifi.dbs.elki.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Multi-Associative container, that stores a list of values for a particular key.
 * 
 * @author Erich Schubert
 *
 * @param <K> Key type
 * @param <V> Value type
 */
// TODO: use MultiValueMap from apache collections instead?
public class HashMapList<K, V> extends HashMap<K, List<V>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 3883242025598456055L;

  /**
   * Constructor.
   */
  public HashMapList() {
    super();
  }

  /**
   * Constructor with initial capacity (of the hash)
   * 
   * @param initialCapacity initial capacity
   */
  public HashMapList(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Add a single value to the given key.
   * 
   * @param key Key
   * @param value Additional Value
   */
  public synchronized void add(K key, V value) {
    List<V> list = super.get(key);
    if (list == null) {
      list = new ArrayList<V>(1);
      super.put(key, list);
    }
    list.add(value);
  }

  /**
   * Check that there is at least one value for the key.
   */
  @Override
  public boolean containsKey(Object key) {
    List<V> list = super.get(key);
    if (list == null) {
      return false;
    }
    return list.size() > 0;
  }

  /**
   * Remove a single value from the map.
   * 
   * @param key Key to remove
   * @param value Value to remove.
   * @return <tt>true</tt> if this list contained the specified element
   */
  public synchronized boolean remove(K key, V value) {
    List<V> list = super.get(key);
    if (list == null) {
      return false;
    }
    boolean success = list.remove(value);
    // remove empty lists.
    if (list.size() == 0) {
      super.remove(key);
    }
    return success;
  }

  /**
   * Test if a given value is already present for the key.
   * 
   * @param key Key
   * @param value Value
   * @return <tt>true</tt> if the keys list contains the specified element
   */
  public boolean contains(K key, V value) {
    List<V> list = super.get(key);
    if (list == null) {
      return false;
    }
    return list.contains(value);
  }
}
