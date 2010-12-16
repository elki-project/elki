package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
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
 * @apiviz.has PCAFilteredRunner
 * @apiviz.has WritableDataStore
 * 
 * @param <NV> Vector type
 */
@Title("Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database.")
public abstract class AbstractSubspaceProjectionIndex<NV extends NumberVector<?, ?>, D extends Distance<D>, P extends ProjectionResult> extends AbstractPreprocessorIndex<NV, P> implements SubspaceProjectionIndex<NV, P> {
  /**
   * Database we are attached to
   */
  final protected Database<NV> database;

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
   * @param database Database to use
   */
  public AbstractSubspaceProjectionIndex(Database<NV> database, D epsilon, DistanceFunction<NV, D> rangeQueryDistanceFunction, int minpts) {
    super();
    this.database = database;
    this.epsilon = epsilon;
    this.rangeQueryDistanceFunction = rangeQueryDistanceFunction;
    this.minpts = minpts;
  }

  /**
   * Preprocessing step.
   */
  protected void preprocess() {
    if(database == null || database.size() <= 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }
    if(storage != null) {
      // Preprocessor was already run.
      return;
    }
    storage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, ProjectionResult.class);

    long start = System.currentTimeMillis();
    RangeQuery<NV, D> rangeQuery = database.getRangeQuery(rangeQueryDistanceFunction);

    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress(this.getClass().getName(), database.size(), getLogger()) : null;
    Iterator<DBID> it = database.iterator();
    while(it.hasNext()) {
      DBID id = it.next();
      List<DistanceResultPair<D>> neighbors = rangeQuery.getRangeForDBID(id, epsilon);

      final P pres;
      if(neighbors.size() >= minpts) {
        pres = computeProjection(id, neighbors, database);
      }
      else {
        DistanceResultPair<D> firstQR = neighbors.get(0);
        neighbors = new ArrayList<DistanceResultPair<D>>();
        neighbors.add(firstQR);
        pres = computeProjection(id, neighbors, database);
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
    if (storage == null) {
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
   * @param database the database for which the preprocessing is performed
   * 
   * @return local subspace projection
   */
  protected abstract P computeProjection(DBID id, List<DistanceResultPair<D>> neighbors, Database<NV> database);

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
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to {@link LocallyWeightedDistanceFunction
     * LocallyWeightedDistanceFunction}.
     * <p>
     * Key: {@code -epsilon}
     * </p>
     */
    public final DistanceParameter<D> EPSILON_PARAM;

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     * <p>
     * Key: {@code -projdbscan.minpts}
     * </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(AbstractProjectedDBSCAN.MINPTS_ID, new GreaterConstraint(0));

    /**
     * Parameter distance function
     */
    private final ObjectParameter<DistanceFunction<NV, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<NV, D>>(AbstractProjectedDBSCAN.INNER_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

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
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super();
      config = config.descend(this);

      // parameter range query distance function
      if(config.grab(DISTANCE_FUNCTION_PARAM)) {
        rangeQueryDistanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
      }

      EPSILON_PARAM = new DistanceParameter<D>(AbstractProjectedDBSCAN.EPSILON_ID, rangeQueryDistanceFunction != null ? rangeQueryDistanceFunction.getDistanceFactory() : null);
      // parameter epsilon
      if(config.grab(EPSILON_PARAM)) {
        epsilon = EPSILON_PARAM.getValue();
      }

      // parameter minpts
      if(config.grab(MINPTS_PARAM)) {
        minpts = MINPTS_PARAM.getValue();
      }
    }

    @Override
    public abstract I instantiate(Database<NV> database);
  }
}