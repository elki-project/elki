package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for a local PCA based index.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.has DistanceFunction
 * 
 * @param <NV> Vector type
 */
@Title("Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database.")
public abstract class AbstractSubspaceProjectionIndex<NV extends NumberVector<?, ?>, D extends Distance<D>, P extends ProjectionResult> extends AbstractPreprocessorIndex<NV, P> implements SubspaceProjectionIndex<NV, P> {
  /**
   * Contains the value of parameter epsilon;
   */
  protected D epsilon;

  /**
   * The distance function for the variance analysis.
   */
  protected DistanceFunction<NV, D> rangeQueryDistanceFunction;

  /**
   * Holds the value of parameter minpts.
   */
  protected int minpts;

  /**
   * Constructor.
   *
   * @param relation Relation
   * @param epsilon Maximum Epsilon
   * @param rangeQueryDistanceFunction range query
   * @param minpts Minpts
   */
  public AbstractSubspaceProjectionIndex(Relation<NV> relation, D epsilon, DistanceFunction<NV, D> rangeQueryDistanceFunction, int minpts) {
    super(relation);
    this.epsilon = epsilon;
    this.rangeQueryDistanceFunction = rangeQueryDistanceFunction;
    this.minpts = minpts;
  }

  /**
   * Preprocessing step.
   */
  protected void preprocess() {
    if(relation == null || relation.size() <= 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }
    if(storage != null) {
      // Preprocessor was already run.
      return;
    }
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, ProjectionResult.class);

    long start = System.currentTimeMillis();
    RangeQuery<NV, D> rangeQuery = relation.getDatabase().getRangeQuery(relation, rangeQueryDistanceFunction);

    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress(this.getClass().getName(), relation.size(), getLogger()) : null;
    for(DBID id : relation.iterDBIDs()) {
      List<DistanceResultPair<D>> neighbors = rangeQuery.getRangeForDBID(id, epsilon);

      final P pres;
      if(neighbors.size() >= minpts) {
        pres = computeProjection(id, neighbors, relation);
      }
      else {
        DistanceResultPair<D> firstQR = neighbors.get(0);
        neighbors = new ArrayList<DistanceResultPair<D>>();
        neighbors.add(firstQR);
        pres = computeProjection(id, neighbors, relation);
      }
      storage.put(id, pres);

      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }
    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }

    long end = System.currentTimeMillis();
    // TODO: re-add timing code!
    if(true) {
      long elapsedTime = end - start;
      getLogger().verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  @Override
  public P getLocalProjection(DBID objid) {
    if(storage == null) {
      preprocess();
    }
    return storage.get(objid);
  }

  /**
   * This method implements the type of variance analysis to be computed for a
   * given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel
   * variance analysis.
   * 
   * @param id the given point
   * @param neighbors the neighbors as query results of the given point
   * @param relation the database for which the preprocessing is performed
   * 
   * @return local subspace projection
   */
  protected abstract P computeProjection(DBID id, List<DistanceResultPair<D>> neighbors, Relation<NV> relation);

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractSubspaceProjectionIndex oneway - - «create»
   */
  public static abstract class Factory<NV extends NumberVector<?, ?>, D extends Distance<D>, I extends AbstractSubspaceProjectionIndex<NV, D, ?>> implements SubspaceProjectionIndex.Factory<NV, I>, Parameterizable {
    /**
     * Contains the value of parameter epsilon;
     */
    protected D epsilon;

    /**
     * The distance function for the variance analysis.
     */
    protected DistanceFunction<NV, D> rangeQueryDistanceFunction;

    /**
     * Holds the value of parameter minpts.
     */
    protected int minpts;

    /**
     * Constructor.
     * 
     * @param epsilon
     * @param rangeQueryDistanceFunction
     * @param minpts
     */
    public Factory(D epsilon, DistanceFunction<NV, D> rangeQueryDistanceFunction, int minpts) {
      super();
      this.epsilon = epsilon;
      this.rangeQueryDistanceFunction = rangeQueryDistanceFunction;
      this.minpts = minpts;
    }

    @Override
    public abstract I instantiate(Relation<NV> relation);

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static abstract class Parameterizer<NV extends NumberVector<?, ?>, D extends Distance<D>, C> extends AbstractParameterizer {
      /**
       * Contains the value of parameter epsilon;
       */
      protected D epsilon = null;

      /**
       * The distance function for the variance analysis.
       */
      protected DistanceFunction<NV, D> rangeQueryDistanceFunction = null;

      /**
       * Holds the value of parameter minpts.
       */
      protected int minpts = 0;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        configRangeQueryDistanceFunction(config);
        configEpsilon(config, rangeQueryDistanceFunction);
        configMinPts(config);
      }

      protected void configRangeQueryDistanceFunction(Parameterization config) {
        ObjectParameter<DistanceFunction<NV, D>> rangeQueryDistanceP = new ObjectParameter<DistanceFunction<NV, D>>(AbstractProjectedDBSCAN.INNER_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
        if(config.grab(rangeQueryDistanceP)) {
          rangeQueryDistanceFunction = rangeQueryDistanceP.instantiateClass(config);
        }
      }

      protected void configEpsilon(Parameterization config, DistanceFunction<NV, D> rangeQueryDistanceFunction) {
        D distanceParser = rangeQueryDistanceFunction != null ? rangeQueryDistanceFunction.getDistanceFactory() : null;
        DistanceParameter<D> epsilonP = new DistanceParameter<D>(AbstractProjectedDBSCAN.EPSILON_ID, distanceParser);
        // parameter epsilon
        if(config.grab(epsilonP)) {
          epsilon = epsilonP.getValue();
        }
      }

      protected void configMinPts(Parameterization config) {
        IntParameter minptsP = new IntParameter(AbstractProjectedDBSCAN.MINPTS_ID, new GreaterConstraint(0));
        if(config.grab(minptsP)) {
          minpts = minptsP.getValue();
        }
      }
    }
  }
}