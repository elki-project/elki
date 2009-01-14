package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Shared Nearest Neighbor Distance Function
 * 
 * Note: this function doesn't satisfy the triangle equality, making it
 * unsuitable for M-Tree!
 *
 * @author Erich Schubert
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public class SNNDistanceFunction<V extends FeatureVector<V,?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * The similarity function.
   */
  private FractionalSharedNearestNeighborSimilarityFunction<V, DoubleDistance> similarityFunction = new FractionalSharedNearestNeighborSimilarityFunction<V, DoubleDistance>();

  public SNNDistanceFunction() {
    super();
  }

  public DoubleDistance distance(V v1, V v2) {
    DoubleDistance sim = similarityFunction.similarity(v1, v2);
    return new DoubleDistance(1.0 - sim.getValue());
  }

  @Override
  public String parameterDescription() {
    return "SNN based distance for feature vectors. " +
        super.parameterDescription();
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    remainingParameters = similarityFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  @Override
  public void setDatabase(Database<V> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    similarityFunction.setDatabase(database, verbose, time);
  }
}
