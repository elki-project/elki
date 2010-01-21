package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import de.lmu.ifi.dbs.elki.utilities.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.IterableIteratorAdapter;

/**
 * Result class providing an ordering backed by a hashmap.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data type in hash map
 */
public class OrderingFromHashMap<T extends Comparable<T>> implements OrderingResult {
  /**
   * HashMap with object values
   */
  protected HashMap<Integer, T> map;

  /**
   * Comparator to use when sorting
   */
  protected Comparator<T> comparator;

  /**
   * Factor for ascending (+1) and descending (-1) ordering.
   */
  int ascending;

  /**
   * Internal comparator, accessing the map to sort objects
   * 
   * @author Erich Schubert
   */
  protected final class ImpliedComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer id1, Integer id2) {
      T k1 = map.get(id1);
      T k2 = map.get(id2);
      assert (k1 != null);
      assert (k2 != null);
      return ascending * k1.compareTo(k2);
    }
  }

  /**
   * Internal comparator, accessing the map but then using the provided
   * comparator to sort objects
   * 
   * @author Erich Schubert
   * 
   */
  protected final class DerivedComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer id1, Integer id2) {
      T k1 = map.get(id1);
      T k2 = map.get(id2);
      assert (k1 != null);
      assert (k2 != null);
      return ascending * comparator.compare(k1, k2);
    }
  }

  /**
   * Constructor with comparator
   * 
   * @param map data hash map
   * @param comparator comparator to use, may be null
   * @param descending ascending (false) or descending (true) order.
   */
  public OrderingFromHashMap(HashMap<Integer, T> map, Comparator<T> comparator, boolean descending) {
    this.map = map;
    this.comparator = comparator;
    this.ascending = descending ? -1 : 1;
  }

  /**
   * Constructor without comparator
   * 
   * @param map data hash map
   * @param descending ascending (false) or descending (true) order.
   */
  public OrderingFromHashMap(HashMap<Integer, T> map, boolean descending) {
    this.map = map;
    this.comparator = null;
    this.ascending = descending ? -1 : 1;
  }

  /**
   * Minimal Constructor
   * 
   * @param map data hash map
   */
  public OrderingFromHashMap(HashMap<Integer, T> map) {
    this.map = map;
    this.comparator = null;
    this.ascending = 1;
  }

  /**
   * Sort the given collection according to this map.
   */
  @Override
  public IterableIterator<Integer> iter(Collection<Integer> ids) {
    ArrayList<Integer> sorted = new ArrayList<Integer>(ids);
    if(comparator != null) {
      Collections.sort(sorted, new DerivedComparator());
    }
    else {
      Collections.sort(sorted, new ImpliedComparator());
    }
    return new IterableIteratorAdapter<Integer>(sorted);
  }

  @Override
  public String getName() {
    return "order";
  }
}
