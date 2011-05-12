package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Heap used for KNN management.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KNNList oneway - - serializes to
 * 
 * @param <D> distance type
 */
public class KNNHeap<D extends Distance<D>> extends TiedTopBoundedHeap<DistanceResultPair<D>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Maximum distance, usually infiniteDistance
   */
  private final D maxdist;

  /**
   * Constructor.
   * 
   * @param k k Parameter
   * @param maxdist k-distance to return for less than k neighbors - usually
   *        infiniteDistance
   */
  public KNNHeap(int k, D maxdist) {
    super(k, new Comp<D>());
    this.maxdist = maxdist;
  }

  /**
   * Simplified constructor. Will return {@code null} as kNN distance with less
   * than k entries.
   * 
   * @param k k Parameter
   */
  public KNNHeap(int k) {
    this(k, null);
  }

  @Override
  public ArrayList<DistanceResultPair<D>> toSortedArrayList() {
    ArrayList<DistanceResultPair<D>> list = super.toSortedArrayList();
    Collections.reverse(list);
    return list;
  }

  /**
   * Serialize to a {@link KNNList}. This empties the heap!
   * 
   * @return KNNList with the heaps contents.
   */
  public KNNList<D> toKNNList() {
    return new KNNList<D>(this, maxdist);
  }

  /**
   * Get the K parameter ("maxsize" internally).
   * 
   * @return K
   */
  public int getK() {
    return super.getMaxSize();
  }

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  public D getKNNDistance() {
    if(size() < getK()) {
      return maxdist;
    }
    return peek().getDistance();
  }

  /**
   * Get maximum distance in heap
   */
  public D getMaximumDistance() {
    if(isEmpty()) {
      return maxdist;
    }
    return peek().getDistance();
  }

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   * @return success code
   */
  public boolean add(D distance, DBID id) {
    if(size() < maxsize || peek().getDistance().compareTo(distance) >= 0) {
      return super.add(new GenericDistanceResultPair<D>(distance, id));
    }
    return true; /* "success" */
  }

  /**
   * Comparator to use.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Comp<D extends Distance<D>> implements Comparator<DistanceResultPair<D>> {
    @Override
    public int compare(DistanceResultPair<D> o1, DistanceResultPair<D> o2) {
      return -o1.getDistance().compareTo(o2.getDistance());
    }
  }
}