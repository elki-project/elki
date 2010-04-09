package de.lmu.ifi.dbs.elki.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * A wrapper class for storing the k most similar comparable objects.
 * 
 * @author Elke Achtert
 * @param <D> Distance class
 */
public class KNNList<D extends Distance<D>> {
  /**
   * The underlying set.
   */
  private SortedSet<DistanceResultPair<D>> list;

  /**
   * The maximum size of this list. In tie situations the size can exceed k.
   */
  private int k;

  /**
   * The infinite distance.
   */
  private D infiniteDistance;

  /**
   * Creates a new KNNList with the specified parameters.
   * 
   * @param k the number k of objects to be stored. In tie situations the size
   *        can exceed k.
   * @param infiniteDistance the infinite distance
   */
  public KNNList(int k, D infiniteDistance) {
    this.list = new TreeSet<DistanceResultPair<D>>();
    this.k = k;
    this.infiniteDistance = infiniteDistance;
  }

  /**
   * Adds a new object to this list if this list has less than k entries.
   * 
   * If this list contains already k (or more) entries tie situations will be
   * resolved in the following way:
   * 
   * If the distance of o is equal to the distance of the last entry, o will be
   * added. I.e. the size of this list will exceed k.
   * 
   * If the distance of o is less than the distance of the last n entries, o
   * will be added. Afterwards the last n entries will be deleted if the new
   * size of this list minus n is equal to or greater than k.
   * 
   * @param o the object to be added
   * @return true, if o has been added, false otherwise.
   */
  public boolean add(DistanceResultPair<D> o) {
    // list has less than k entries or
    // the last distance equals the distance of o
    if(list.size() < k || o.getDistance().compareTo(list.last().getDistance()) == 0) {
      list.add(o);
      return true;
    }

    DistanceResultPair<D> last = list.last();
    D lastDist = last.getDistance();

    if(o.getDistance().compareTo(lastDist) < 0) {
      SortedSet<DistanceResultPair<D>> lastList = list.subSet(new DistanceResultPair<D>(lastDist, 0), new DistanceResultPair<D>(lastDist, Integer.MAX_VALUE));

      int llSize = lastList.size();
      if(list.size() - llSize >= k - 1) {
        for(int i = 0; i < llSize; i++) {
          list.remove(list.last());
        }
      }
      list.add(o);
      return true;
    }

    return false;
  }

  /**
   * Returns the k-th distance of this list (e.g. the key of the k-th element).
   * If this list is empty or contains less than k elements, an infinite key
   * will be returned.
   * 
   * @return the maximum distance of this list
   */
  public D getKNNDistance() {
    if(list.size() < k) {
      return infiniteDistance;
    }
    return getMaximumDistance();
  }

  /**
   * Returns the maximum distance of this list (e.g. the key of the last
   * element). If this list is empty an infinite key will be returned.
   * 
   * @return the maximum distance of this list
   */
  public D getMaximumDistance() {
    if(list.isEmpty()) {
      return infiniteDistance;
    }
    DistanceResultPair<D> last = list.last();
    return last.getDistance();
  }

  /**
   * Returns a list representation of this KList.
   * 
   * @return a list representation of this KList
   */
  public List<DistanceResultPair<D>> toList() {
    return new ArrayList<DistanceResultPair<D>>(list);
  }

  /**
   * Return a list containing just the distances
   * 
   * @return list of distances
   */
  public List<D> distancesToList() {
    List<D> knnDistances = new ArrayList<D>();
    List<DistanceResultPair<D>> qr = toList();

    for(int i = 0; i < qr.size() && i < k; i++) {
      knnDistances.add(qr.get(i).getDistance());
    }

    for(int i = qr.size(); i < k; i++) {
      knnDistances.add(infiniteDistance);
    }

    return knnDistances;
  }

  /**
   * Return a list containing only the object IDs
   * 
   * @return list of object ids
   */
  public List<Integer> idsToList() {
    List<Integer> ids = new ArrayList<Integer>(k);
    List<DistanceResultPair<D>> qr = toList();
    for(int i = 0; i < qr.size() && i < k; i++) {
      ids.add(qr.get(i).getID());
    }
    return ids;
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
  @Override
  public String toString() {
    return list + " , knn-dist = " + getKNNDistance();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj argument;
   *         <code>false</code> otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }
    final KNNList<D> knnList = ClassGenericsUtil.castWithGenericsOrNull(KNNList.class, o);
    if(knnList == null) {
      return false;
    }

    if(k != knnList.k) {
      return false;
    }
    Iterator<DistanceResultPair<D>> it = list.iterator();
    Iterator<DistanceResultPair<D>> other_it = knnList.list.iterator();

    while(it.hasNext()) {
      DistanceResultPair<D> next = it.next();
      DistanceResultPair<D> other_next = other_it.next();

      if(!next.equals(other_next)) {
        return false;
      }

    }
    return list.equals(knnList.list);
  }

  /**
   * Combine list hash code with the value of k.
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int result;
    result = list.hashCode();
    result = 29 * result + k;
    return result;
  }
}