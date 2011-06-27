package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDatabaseDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Adapter from a normalized similarity function to a distance function.
 * 
 * Note: The derived distance function will usually not satisfy the triangle
 * equation.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 * 
 * @param <O> object class to process
 */
public abstract class AbstractSimilarityAdapter<O> extends AbstractDatabaseDistanceFunction<O, DoubleDistance> {
  /**
   * Parameter to specify the similarity function to derive the distance between
   * database objects from. Must extend
   * {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction}
   * .
   * <p>
   * Key: {@code -adapter.similarityfunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction}
   * </p>
   */
  public static final OptionID SIMILARITY_FUNCTION_ID = OptionID.getOrCreateOptionID("adapter.similarityfunction", "Similarity function to derive the distance between database objects from.");

  /**
   * Holds the similarity function.
   */
  protected NormalizedSimilarityFunction<? super O, ? extends NumberDistance<?, ?>> similarityFunction;

  /**
   * Constructor.
   * 
   * @param similarityFunction Similarity function to use.
   */
  public AbstractSimilarityAdapter(NormalizedSimilarityFunction<? super O, ? extends NumberDistance<?, ?>> similarityFunction) {
    super();
    this.similarityFunction = similarityFunction;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return similarityFunction.getInputTypeRestriction();
  }

  @Override
  public boolean isSymmetric() {
    return similarityFunction.isSymmetric();
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  abstract public <T extends O> DistanceQuery<T, DoubleDistance> instantiate(Relation<T> database);

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    // Same subclass
    if(!this.getClass().equals(obj.getClass())) {
      return false;
    }
    // Same similarity function
    AbstractSimilarityAdapter<?> other = (AbstractSimilarityAdapter<?>) obj;
    return other.similarityFunction.equals(other.similarityFunction);
  }

  /**
   * Inner proxy class for SNN distance function.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   */
  public abstract static class Instance<O> extends AbstractDatabaseDistanceFunction.Instance<O, DoubleDistance> {
    /**
     * The similarity query we use.
     */
    private SimilarityQuery<? super O, ? extends NumberDistance<?, ?>> similarityQuery;

    /**
     * Constructor.
     * 
     * @param database Database to use
     * @param parent Parent distance function
     * @param similarityQuery Similarity query
     */
    public Instance(Relation<O> database, DistanceFunction<? super O, DoubleDistance> parent, SimilarityQuery<? super O, ? extends NumberDistance<?, ?>> similarityQuery) {
      super(database, parent);
      this.similarityQuery = similarityQuery;
    }

    /**
     * Transformation function.
     * 
     * @param similarity Similarity value
     * @return Distance value
     */
    public abstract double transform(double similarity);

    @Override
    public DoubleDistance distance(DBID id1, DBID id2) {
      final NumberDistance<?, ?> sim = similarityQuery.similarity(id1, id2);
      return new DoubleDistance(transform(sim.doubleValue()));
    }

    @Override
    public DoubleDistance getDistanceFactory() {
      return DoubleDistance.FACTORY;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Holds the similarity function.
     */
    protected NormalizedSimilarityFunction<? super O, ? extends NumberDistance<?, ?>> similarityFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<NormalizedSimilarityFunction<? super O, ? extends NumberDistance<?, ?>>> param = new ObjectParameter<NormalizedSimilarityFunction<? super O, ? extends NumberDistance<?, ?>>>(SIMILARITY_FUNCTION_ID, NormalizedSimilarityFunction.class, FractionalSharedNearestNeighborSimilarityFunction.class);
      if(config.grab(param)) {
        similarityFunction = param.instantiateClass(config);
      }
    }
  }
}