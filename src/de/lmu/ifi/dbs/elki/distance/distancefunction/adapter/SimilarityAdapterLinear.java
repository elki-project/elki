package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Adapter from a normalized similarity function to a distance function using <code>1 - sim</code>.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class to process.
 */
public class SimilarityAdapterLinear<O extends DatabaseObject> extends SimilarityAdapterAbstract<O> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public SimilarityAdapterLinear(Parameterization config) {
    super(config);
  }

  /**
   * Distance implementation
   */
  @Override
  public DoubleDistance distance(O v1, O v2) {
    DoubleDistance sim = similarityFunction.similarity(v1, v2);
    return new DoubleDistance(1.0 - sim.doubleValue());
  }
}
