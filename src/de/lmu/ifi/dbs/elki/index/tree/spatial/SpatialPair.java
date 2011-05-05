package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Defines the requirements for objects that can be indexed by a Spatial Index,
 * which are spatial nodes or data objects.
 * 
 * @author Elke Achtert
 */
public class SpatialPair<K, V extends SpatialComparable> extends Pair<K, V> implements SpatialComparable {
  /**
   * Constructor: bundle a key and a spatial comparable
   * 
   * @param key key
   * @param spatial spatial value
   */
  public SpatialPair(K key, V spatial) {
    super(key, spatial);
  }

  @Override
  public int getDimensionality() {
    return second.getDimensionality();
  }

  @Override
  public double getMin(int dimension) {
    return second.getMin(dimension);
  }

  @Override
  public double getMax(int dimension) {
    return second.getMax(dimension);
  }
}