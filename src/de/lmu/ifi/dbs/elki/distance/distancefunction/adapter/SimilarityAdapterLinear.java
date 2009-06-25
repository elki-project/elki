package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Adapter from a normalized similarity function to a distance function using <code>1 - sim</code>.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector class to process.
 */
public class SimilarityAdapterLinear<V extends FeatureVector<V,?>> extends SimilarityAdapterAbstract<V> {
  /**
   * Distance implementation
   */
  @Override
  public DoubleDistance distance(V v1, V v2) {
    DoubleDistance sim = similarityFunction.similarity(v1, v2);
    return new DoubleDistance(1.0 - sim.getValue());
  }
}
