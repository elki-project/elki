package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A wrapper class for storing the k most similar comparable objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KNNList<D extends Distance>{
  /**
   * The underlying set.
   */
  private SortedSet<QueryResult<D>> list;

  /**
   * The maximum size of this list.
   */
  private int k;

  /**
   * The infinite distance.
   */
  private D infiniteDistance;

  /**
   * Creates a new KNNList with the specified parameters.
   *
   * @param k           the number k of objects to be stored
   * @param infiniteDistance the infinite distance
   */
  public KNNList(int k, D infiniteDistance) {
    this.list = new TreeSet<QueryResult<D>>();
    this.k = k;
    this.infiniteDistance = infiniteDistance;
  }

  /**
   * Adds a new object to this list. If this list contains already
   * k entries and the key of the specified object o is less than
   * the key of the last entry, the last entry will be deleted.
   *
   * @param o the object to be added
   * @return true, if o has been added, false otherwise.
   */
  public boolean add(QueryResult<D> o) {
    if (list.size() < k) {
      list.add(o);
      return true;
    }

    QueryResult<D> last = list.last();

    if (o.compareTo(last) < 0) {
      list.remove(last);
      list.add(o);
      return true;
    }

    return false;
  }

  /**
   * Returns the maximum distance of this list (e.g. the key
   * of the last element). If this list is empty an infinite key will
   * be returned.
   *
   * @return the maximum distance of this list
   */
  public D getMaximumDistance() {
    if (list.isEmpty())
      return infiniteDistance;

    QueryResult<D> last = list.last();
    return last.getKey();
  }

  /**
   * Returns a list representation of this KList.
   *
   * @return a list representation of this KList
   */
  public List<QueryResult<D>> toList() {
    return new ArrayList<QueryResult<D>>(list);
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
    return list + " , knn-dist = " + getMaximumDistance();
  }
}
