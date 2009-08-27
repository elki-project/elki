package experimentalcode.marisa.utils;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

public class MyKNNList<D extends Distance<D>> extends KNNList<D> {

  private PriorityQueue<Integer> pq;

  /**
   * The infinite distance. (the parent class's is private)
   */
  private D infiniteDistance;

  public MyKNNList(int k, D infiniteDistance) {
    super(k, infiniteDistance);
    this.infiniteDistance = infiniteDistance;
    pq = new PriorityQueue<Integer>(false, k);
  }

  @Override
  public boolean add(DistanceResultPair<D> o) {
    // TODO: generalize
    double priority = ((DoubleDistance) o.getDistance()).getValue();
    if(pq.size() < getK()) {
      pq.add(priority, o.getID());
      return true;
    }
    if(pq.firstPriority() >= priority) {
      pq.removeFirst();
      pq.add(priority, o.getID());
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
  @Override
  public D getKNNDistance() {
    if(pq.size() < getK()) {
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
  @Override
  public D getMaximumDistance() {
    if(pq.isEmpty()) {
      return infiniteDistance;
    }
    // TODO: generalize
    return (D) new DoubleDistance(pq.firstPriority());
  }

  /**
   * Returns a list representation of this KList.
   * 
   * @return a list representation of this KList
   */
  @Override
  public List<DistanceResultPair<D>> toList() {
    List<DistanceResultPair<D>> l = new ArrayList<DistanceResultPair<D>>(pq.size());
    PriorityQueue<Integer> pqCopy = pq.copy();
    while(!pqCopy.isEmpty()) {
      l.add(0,new DistanceResultPair<D>(
      // TODO: generalize
      (D) new DoubleDistance(pqCopy.firstPriority()), pqCopy.removeFirst()));
    }
    return l;
  }

  /**
   * Return a list containing just the distances
   * 
   * @return list of distances
   */
  @Override
  public List<D> distancesToList() {
    List<D> l = new ArrayList<D>(pq.size());
    PriorityQueue<Integer> pqCopy = pq.copy();
    while(!pqCopy.isEmpty()) {
      l.add(0, // TODO: generalize
      (D) new DoubleDistance(pqCopy.firstPriority()));
    }
    return l;
  }

  /**
   * Return a list containing only the object IDs
   * 
   * @return list of object ids
   */
  @Override
  public List<Integer> idsToList() {
    List<Integer> l = new ArrayList<Integer>(pq.size());
    PriorityQueue<Integer> pqCopy = pq.copy();
    while(!pqCopy.isEmpty()) {
      l.add(0, pqCopy.removeFirst());
    }
    return l;
  }

  /**
   * Returns the current size of this list.
   * 
   * @return the current size of this list
   */
  @Override
  public int size() {
    return pq.size();
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return idsToList() + " , knn-dist = " + getKNNDistance();
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
    if (getK() != ((MyKNNList) o).getK())
      return false;
    PriorityQueue<Integer> copy = pq.copy();
    PriorityQueue<Integer> oCopy = ((MyKNNList) o).pq.copy();
    if (copy.size() != oCopy.size())
      return false;
    while (!copy.isEmpty()) {
      if (copy.firstPriority() != oCopy.firstPriority())
        return false;
      if (!copy.removeFirst().equals(oCopy.removeFirst()))
        return false;
    }
    return true;
  }

  /**
   * Combine list hash code with the value of k.
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int result;
    result = pq.hashCode();
    result = 29 * result + getK();
    return result;
  }
}
