package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

import java.util.*;

/**
 * A wrapper class for storing the k most similar comparable objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KNNList<D extends Distance> {
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
   * @param k                the number k of objects to be stored
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
    D lastKey = last.getDistance();

    if (o.getDistance().compareTo(last.getDistance()) < 0) {
      SortedSet<QueryResult<D>> lastList = list.subSet(new QueryResult<D>(0, lastKey),
                                                       new QueryResult<D>(Integer.MAX_VALUE, lastKey));

      int llSize = lastList.size();
      if (list.size() - llSize >= k - 1) {
        for (int i = 0; i < llSize; i++)
          list.remove(list.last());
      }
      list.add(o);
      return true;
    }

    if (o.getDistance().compareTo(last.getDistance()) == 0) {
      list.add(o);
      return true;
    }

    return false;
  }

  /**
   * Returns the k-th distance of this list (e.g. the key
   * of the k-th element). If this list is empty or contains less than
   * k elements, an infinite key will be returned.
   *
   * @return the maximum distance of this list
   */
  public D getKNNDistance() {
    if (list.size() < k) return infiniteDistance;
    return getMaximumDistance();
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
    return last.getDistance();
  }

  /**
   * Returns a list representation of this KList.
   *
   * @return a list representation of this KList
   */
  public List<QueryResult<D>> toList() {
    return new ArrayList<QueryResult<D>>(list);
  }

  public List<D> distancesToList() {
    List<D> knnDistances = new ArrayList<D>();
    List<QueryResult<D>> qr = toList();

    for (QueryResult<D> result : qr) {
      knnDistances.add(result.getDistance());
    }

    for (int i = qr.size(); i < k; i++) {
      knnDistances.add(infiniteDistance);
    }

    return knnDistances;
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
   * Returns the maximum size of this list.
   *
   * @return the maximum size of this list
   */
  public int getK() {
    return k;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return list + " , knn-dist = " + getKNNDistance();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj
   *         argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final KNNList<D> knnList = (KNNList<D>) o;

    if (k != knnList.k) return false;


    Iterator<QueryResult<D>> it = list.iterator();
    Iterator<QueryResult<D>> other_it = knnList.list.iterator();

    while (it.hasNext()) {
      QueryResult<D> next = it.next();
      QueryResult<D> other_next = other_it.next();

      if (! next.equals(other_next)) {
        System.out.println("next " + next);
        System.out.println("other_next " + other_next);
        return false;
      }


    }
    if (!list.equals(knnList.list)) {
      System.out.println("list");
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result;
    result = list.hashCode();
    result = 29 * result + k;
    return result;
  }
}
