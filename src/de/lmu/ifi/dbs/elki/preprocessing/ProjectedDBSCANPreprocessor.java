package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract superclass for preprocessor of algorithms extending the
 * ProjectedDBSCAN algorithm.
 * 
 * @author Arthur Zimek
 * @param <D> Distance type
 * @param <V> Vector type
 */
public abstract class ProjectedDBSCANPreprocessor<D extends Distance<D>, V extends NumberVector<? extends V, ?>, R extends ProjectionResult> extends AbstractLoggable implements LocalProjectionPreprocessor<V, R> {
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
  private final IntParameter MINPTS_PARAM = new IntParameter(ProjectedDBSCAN.MINPTS_ID, new GreaterConstraint(0));

  /**
   * Parameter distance function
   */
  private final ObjectParameter<DistanceFunction<V, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<V, D>>(ProjectedDBSCAN.INNER_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Contains the value of parameter epsilon;
   */
  protected D epsilon;

  /**
   * The distance function for the variance analysis.
   */
  protected DistanceFunction<V, D> rangeQueryDistanceFunction;

  /**
   * Holds the value of parameter minpts.
   */
  protected int minpts;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  protected ProjectedDBSCANPreprocessor(Parameterization config) {
    super();
    config = config.descend(this);
    // parameter range query distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      rangeQueryDistanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }

    EPSILON_PARAM = new DistanceParameter<D>(ProjectedDBSCAN.EPSILON_ID, rangeQueryDistanceFunction != null ? rangeQueryDistanceFunction.getDistanceFactory() : null);
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
  public <T extends V> Instance<T> instantiate(Database<T> database) {
    return new Instance<T>(database);
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @param <T> The actual data type
   */
  public class Instance<T extends V> implements LocalProjectionPreprocessor.Instance<R> {
    /**
     * Logger to use
     */
    private Logging logger = Logging.getLogger(DiSHPreprocessor.class);

    /**
     * Storage for the precomputed results
     */
    private WritableDataStore<R> pcaStorage = null;

    public Instance(Database<T> database) {
      if(database == null || database.size() <= 0) {
        throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
      }
      if(pcaStorage != null) {
        // Preprocessor was already run.
        return;
      }
      pcaStorage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, ProjectionResult.class);

      long start = System.currentTimeMillis();
      DistanceQuery<T, D> rqdist = database.getDistanceQuery(rangeQueryDistanceFunction);

      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress(this.getClass().getName(), database.size(), logger) : null;
      Iterator<DBID> it = database.iterator();
      while(it.hasNext()) {
        DBID id = it.next();
        List<DistanceResultPair<D>> neighbors = database.rangeQuery(id, epsilon, rqdist);

        final R pcares;
        if(neighbors.size() >= minpts) {
          pcares = runVarianceAnalysis(id, neighbors, database);
        }
        else {
          DistanceResultPair<D> firstQR = neighbors.get(0);
          neighbors = new ArrayList<DistanceResultPair<D>>();
          neighbors.add(firstQR);
          pcares = runVarianceAnalysis(id, neighbors, database);
        }
        pcaStorage.put(id, pcares);

        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
      if(progress != null) {
        progress.ensureCompleted(logger);
      }

      long end = System.currentTimeMillis();
      // TODO: re-add timing code!
      if(true) {
        long elapsedTime = end - start;
        logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
      }
    }

    /**
     * Get the precomputed result for a given value.
     * 
     * @param objid Object ID
     * @return PCA result
     */
    @Override
    public R get(DBID objid) {
      return pcaStorage.get(objid);
    }
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
   * @return filtered PCA result
   */
  protected abstract <T extends V> R runVarianceAnalysis(DBID id, List<DistanceResultPair<D>> neighbors, Database<T> database);
}