package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Adapter from a normalized similarity function to a distance function using arccos(sim)
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector class to process.
 */
public class SimilarityAdapterArccos<V extends FeatureVector<V,?>> extends SimilarityAdapterAbstract<V> {
  /**
   * Distance implementation
   */
  @Override
  public DoubleDistance distance(V v1, V v2) {
    DoubleDistance sim = similarityFunction.similarity(v1, v2);
    return new DoubleDistance(Math.acos(sim.getValue()));
  }
}
