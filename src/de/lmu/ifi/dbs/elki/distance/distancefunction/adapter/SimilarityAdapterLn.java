package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Adapter from a normalized similarity function to a distance function using <code>-log(sim)</code>.
 * 
 * @author Erich Schubert
 *
 * @param <O> object class to process.
 */
public class SimilarityAdapterLn<O extends DatabaseObject> extends SimilarityAdapterAbstract<O> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public SimilarityAdapterLn(Parameterization config) {
    super(config);
  }

  /**
   * Distance implementation
   */
  @Override
  public DoubleDistance distance(O v1, O v2) {
    DoubleDistance sim = similarityFunction.similarity(v1, v2);
    return new DoubleDistance(- Math.log(sim.doubleValue()));
  }
}
