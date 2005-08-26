package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A wrapper class for storing the k nearest neighbors.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KNNList {
  /**
   * The underlying set.
   */
  private SortedSet<QueryResult> list;

  /**
   * The maximum size of this list.
   */
  private int k;

  /**
   * The infinite distance.
   */
  private Distance infiniteDistance;

  /**
   * Creates a new KNNList with the specified parameters.
   * @param k the number k of nearest neighbors to be stored
   * @param infiniteDistance the infinite distance
   */
  public KNNList(int k, Distance infiniteDistance) {
    this.list = new TreeSet<QueryResult>();
    this.k = k;
    this.infiniteDistance = infiniteDistance;
  }

  /**
   * Adds a new query result to this list. If this list contains already
   * k entries and the distance of the specified object o is less than
   * the distance of the last entry, the last entry will be deleted.
   * @param o the query reult to be added
   * @return true, if o has been added, false otherwise.
   */
  public boolean add(QueryResult o) {
    if (list.size() < k) {
      list.add(o);
      return true;
    }

    QueryResult last = list.last();
    if (o.getDistance().compareTo(last.getDistance()) < 0) {
      list.remove(last);
      list.add(o);
      return true;
    }

    return false;
  }

  /**
   * Returns the maximum distance of this list (e.g. the distance
   * of the last element). If this list is empty an infinite distance will
   * be returned.
   * @return the maximum distance of this list
   */
  public Distance getMaximumDistance() {
    if (list.isEmpty())
      return infiniteDistance;

    QueryResult last = list.last();
    return last.getDistance();
  }

  /**
   * Returns a list representation of this KNNList.
   * @return a list representation of this KNNList
   */
  public List<QueryResult> toList() {
    return new ArrayList<QueryResult>(list);
  }

  /**
   * Returns the current size of this list.
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
    return "knns = " + list + " , knn-distance = " + getMaximumDistance();
  }
}
