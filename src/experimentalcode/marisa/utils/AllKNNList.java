package experimentalcode.marisa.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * PQ for maintaining (at least) <code>k</code> objects. In contrast to
 * {@link MyKNNList}, adding an element with the same distance as
 * {@link #getKNNDistance()} does not result in the deletion of the element, but
 * causes a tie-treatment. The number of objects actually maintained in this
 * list can be determined via {@link #size()};
 * 
 * @author Marisa Thoma
 * 
 * @param <D>
 */
public class AllKNNList<D extends Distance<D>> extends MyKNNList<D> {
  private LinkedList<Integer> additionalMaxElements = new LinkedList<Integer>();

  public AllKNNList(int k, D infiniteDistance) {
    super(k, infiniteDistance);
  }

  @Override
  public boolean add(DistanceResultPair<D> o) {
    D priority = o.getDistance();
    if(pq.size() < getK()) {
      pq.add(priority, o.getID());
      return true;
    }
    int comp = pq.firstPriority().compareTo(priority);
    if(comp == 0) {
      additionalMaxElements.add(o.getID());
    }
    else if(comp > 0) {
      pq.removeFirst();
      pq.add(priority, o.getID());
      additionalMaxElements.clear();
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
    return pq.firstPriority();
  }

  /**
   * Returns a list representation of this KList.
   * 
   * @return a list representation of this KList
   */
  @Override
  public List<DistanceResultPair<D>> toList() {
    List<DistanceResultPair<D>> l = new ArrayList<DistanceResultPair<D>>(size());
    PQ<D, Integer> pqCopy = pq.copy();
    while(!pqCopy.isEmpty()) {
      l.add(0, new DistanceResultPair<D>(pqCopy.firstPriority(), pqCopy.removeFirst()));
    }
    for(Iterator<Integer> iterator = additionalMaxElements.iterator(); iterator.hasNext();) {
      Integer id = iterator.next();
      l.add(new DistanceResultPair<D>(getMaximumDistance(), id));
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
    List<D> l = new ArrayList<D>(size());
    PQ<D, Integer> pqCopy = pq.copy();
    while(!pqCopy.isEmpty()) {
      l.add(0, pqCopy.firstPriority());
    }
    for(int i = 0; i < additionalMaxElements.size(); i++) {
      l.add(getMaximumDistance());
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
    List<Integer> l = new ArrayList<Integer>(size());
    PQ<D, Integer> pqCopy = pq.copy();
    while(!pqCopy.isEmpty()) {
      l.add(0, pqCopy.removeFirst());
    }
    l.addAll(additionalMaxElements);
    return l;
  }

  /**
   * Returns the current size of this list.
   * 
   * @return the current size of this list
   */
  @Override
  public int size() {
    return pq.size() + additionalMaxElements.size();
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
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if(!(o instanceof AllKNNList)) {
      return false;
    }
    if(this == o) {
      return true;
    }
    if(o == null || !getClass().equals(o.getClass())) {
      return false;
    }
    if(getK() != ((AllKNNList<?>) o).getK()) {
      return false;
    }
    if(additionalMaxElements.size() != ((AllKNNList<?>) o).additionalMaxElements.size()) {
      return false;
    }
    for(Iterator<Integer> i1 = additionalMaxElements.iterator(), i2 = ((AllKNNList<?>) o).additionalMaxElements.iterator(); i1.hasNext() && i2.hasNext();) {
      if(!i1.next().equals(i2.next())) {
        return false;
      }
    }
    PQ<D, Integer> copy = pq.copy();
    PQ<D, Integer> oCopy = ((AllKNNList<D>) o).pq.copy();
    if(copy.size() != oCopy.size()) {
      return false;
    }
    while(!copy.isEmpty()) {
      if(!copy.firstPriority().equals(oCopy.firstPriority())) {
        return false;
      }
      if(!copy.removeFirst().equals(oCopy.removeFirst())) {
        return false;
      }
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