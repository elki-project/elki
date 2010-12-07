package de.lmu.ifi.dbs.elki.result;

import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;

/**
 * Result class providing an ordering backed by a hashmap.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data type in hash map
 */
public class OrderingFromDataStore<T extends Comparable<T>> extends BasicResult implements OrderingResult {
  /**
   * HashMap with object values
   */
  protected DataStore<? extends T> map;

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
   * 
   * @apiviz.exclude
   */
  protected final class ImpliedComparator implements Comparator<DBID> {
    @Override
    public int compare(DBID id1, DBID id2) {
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
   * @apiviz.exclude
   */
  protected final class DerivedComparator implements Comparator<DBID> {
    @Override
    public int compare(DBID id1, DBID id2) {
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
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param map data hash map
   * @param comparator comparator to use, may be null
   * @param descending ascending (false) or descending (true) order.
   */
  public OrderingFromDataStore(String name, String shortname, DataStore<? extends T> map, Comparator<T> comparator, boolean descending) {
    super(name, shortname);
    this.map = map;
    this.comparator = comparator;
    this.ascending = descending ? -1 : 1;
  }

  /**
   * Constructor without comparator
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param map data hash map
   * @param descending ascending (false) or descending (true) order.
   */
  public OrderingFromDataStore(String name, String shortname, DataStore<? extends T> map, boolean descending) {
    super(name, shortname);
    this.map = map;
    this.comparator = null;
    this.ascending = descending ? -1 : 1;
  }

  /**
   * Minimal Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param map data hash map
   */
  public OrderingFromDataStore(String name, String shortname, DataStore<? extends T> map) {
    super(name, shortname);
    this.map = map;
    this.comparator = null;
    this.ascending = 1;
  }

  /**
   * Sort the given collection according to this map.
   */
  @Override
  public IterableIterator<DBID> iter(DBIDs ids) {
    ArrayModifiableDBIDs sorted = DBIDUtil.newArray(ids);
    if(comparator != null) {
      Collections.sort(sorted, new DerivedComparator());
    }
    else {
      Collections.sort(sorted, new ImpliedComparator());
    }
    return IterableUtil.fromIterable(sorted);
  }
}