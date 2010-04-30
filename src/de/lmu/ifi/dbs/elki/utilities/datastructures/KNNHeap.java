package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.ArrayList;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Heap used for KNN management.
 * 
 * @author Erich Schubert
 *
 * @param <D>
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
   * @param maxdist k-distance to return for less than k neighbors - usually infiniteDistance
   */
  public KNNHeap(int k, D maxdist) {
    super(k, Collections.reverseOrder());
    this.maxdist = maxdist;
  }

  /**
   * Simplified constructor. Will return {@code null} as kNN distance with less than k entries.
   * 
   * @param k k Parameter
   */
  public KNNHeap(int k) {
    this(k, null);
  }

  /** {@inheritDoc} */
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
    if (size() < getK()) {
      return maxdist;
    }
    return peek().getDistance();
  }
  
  /**
   * Get maximum distance in heap
   */
  public D getMaximumDistance() {
    if (isEmpty()) {
      return maxdist;
    }
    return peek().getDistance();
  }
}