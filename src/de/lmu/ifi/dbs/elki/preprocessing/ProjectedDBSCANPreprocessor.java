package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
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
public abstract class ProjectedDBSCANPreprocessor<D extends Distance<D>, V extends NumberVector<V, ?>, R extends ProjectionResult> extends AbstractLoggable implements LocalProjectionPreprocessor<V, R> {
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
   * The default range query distance function.
   */
  public static final Class<?> DEFAULT_DISTANCE_FUNCTION = EuclideanDistanceFunction.class;

  /**
   * Parameter distance function
   */
  private final ObjectParameter<DistanceFunction<V, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<V, D>>(ProjectedDBSCAN.INNER_DISTANCE_FUNCTION_ID, DistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);

  /**
   * Contains the value of parameter epsilon;
   */
  private D epsilon;

  /**
   * The distance function for the variance analysis.
   */
  protected DistanceFunction<V, D> rangeQueryDistanceFunction;

  /**
   * Holds the value of parameter minpts.
   */
  private int minpts;

  /**
   * Storage for the precomputed results
   */
  private HashMap<Integer, R> pcaStorage = new HashMap<Integer, R>();

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  protected ProjectedDBSCANPreprocessor(Parameterization config) {
    super();
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

  public void run(Database<V> database, @SuppressWarnings("unused") boolean verbose, boolean time) {
    if(database == null || database.size() <= 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }

    long start = System.currentTimeMillis();
    rangeQueryDistanceFunction.setDatabase(database);

    FiniteProgress progress = new FiniteProgress(this.getClass().getName(), database.size());
    if(logger.isVerbose()) {
      logger.verbose("Preprocessing:");
    }
    Iterator<Integer> it = database.iterator();
    int processed = 1;
    while(it.hasNext()) {
      Integer id = it.next();
      List<DistanceResultPair<D>> neighbors = database.rangeQuery(id, epsilon, rangeQueryDistanceFunction);

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

      progress.setProcessed(processed++);
      if(logger.isVerbose()) {
        logger.progress(progress);
      }
    }

    long end = System.currentTimeMillis();
    if(time) {
      long elapsedTime = end - start;
      logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
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
  protected abstract R runVarianceAnalysis(Integer id, List<DistanceResultPair<D>> neighbors, Database<V> database);

  /**
   * Get the precomputed result for a given value.
   * 
   * @param objid Object ID
   * @return PCA result
   */
  public R get(Integer objid) {
    return pcaStorage.get(objid);
  }
}