package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Adapter from a normalized similarity function to a distance function using
 * <code>arccos(sim)</code>.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 * 
 * @param <O> Object class to process.
 */
public class SimilarityAdapterArccos<O extends DatabaseObject> extends AbstractSimilarityAdapter<O> {
  /**
   * Constructor.
   * 
   * @param similarityFunction Similarity function
   */
  public SimilarityAdapterArccos(NormalizedSimilarityFunction<? super O, ? extends NumberDistance<?, ?>> similarityFunction) {
    super(similarityFunction);
  }

  @Override
  public <T extends O> DistanceQuery<T, DoubleDistance> instantiate(Database<T> database) {
    SimilarityQuery<T, ? extends NumberDistance<?, ?>> similarityQuery = similarityFunction.instantiate(database);
    return new Instance<T>(database, this, similarityQuery);
  }

  /**
   * Distance function instance
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   */
  public static class Instance<O extends DatabaseObject> extends AbstractSimilarityAdapter.Instance<O> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param parent Parent distance
     * @param similarityQuery similarity Query to use
     */
    public Instance(Database<O> database, DistanceFunction<? super O, DoubleDistance> parent, SimilarityQuery<O, ? extends NumberDistance<?, ?>> similarityQuery) {
      super(database, parent, similarityQuery);
    }

    @Override
    public double transform(double similarity) {
      return Math.acos(similarity);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractSimilarityAdapter.Parameterizer<O> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected SimilarityAdapterArccos<O> makeInstance() {
      return new SimilarityAdapterArccos<O>(similarityFunction);
    }
  }
}