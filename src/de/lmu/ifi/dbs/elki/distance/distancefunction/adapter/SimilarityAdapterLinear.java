package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Adapter from a normalized similarity function to a distance function using
 * <code>1 - sim</code>.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object class to process.
 */
public class SimilarityAdapterLinear<O extends DatabaseObject> extends AbstractSimilarityAdapter<O> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public SimilarityAdapterLinear(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public <T extends O> DistanceQuery<T, DoubleDistance> instantiate(Database<T> database) {
    SimilarityQuery<T, DoubleDistance> similarityQuery = similarityFunction.instantiate(database);
    return new Instance<T>(database, this, similarityQuery);
  }

  /**
   * Distance function instance
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Instance<O extends DatabaseObject> extends AbstractSimilarityAdapter.Instance<O> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param parent Parent distance
     * @param similarityQuery similarity Query to use
     */
    public Instance(Database<O> database, DistanceFunction<? super O, DoubleDistance> parent, SimilarityQuery<O, DoubleDistance> similarityQuery) {
      super(database, parent, similarityQuery);
    }

    @Override
    public double transform(double similarity) {
      return 1.0 - similarity;
    }
  }
}