package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * <p>Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter {@link #K_ID} ({@code -lof.k}).</p>
 * 
 * <p>This implementation diverts from the original LOF publication in that it allows the
 * user to use a different distance function for the reachability distance and neighborhood
 * determination (although the default is to use the same value.)</p>
 * 
 * <p>The k nearest neighbors are determined using the parameter
 * {@link DistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}, while the reference set used in reachability
 * distance computation is configured using {@link #REACHABILITY_DISTANCE_FUNCTION_ID}.</p>
 * 
 * <p>The original LOF parameter was called &quot;minPts&quot;. Since kNN queries in ELKI have slightly
 * different semantics - exactly k neighbors are returned - we chose to rename the parameter to
 * {@link #K_ID} ({@code -lof.k}) to reflect this difference.</p> 
 * 
 * <p>
 * Reference:
 * <br>M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander:
 * LOF: Identifying Density-Based Local Outliers.
 * <br>In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000.
 * </p>
 *
 * @author Peer Kr&ouml;ger
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class LOF<O extends DatabaseObject, D extends NumberDistance<D,?>> extends DistanceBasedAlgorithm<O, D, MultiResult> {
  /**
   * OptionID for {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("lof.reachdistfunction", "Distance function to determine the reachability distance between database objects.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -lof.reachdistfunction}
   * </p>
   */
  private final ClassParameter<DistanceFunction<O, D>> REACHABILITY_DISTANCE_FUNCTION_PARAM = new ClassParameter<DistanceFunction<O, D>>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);

  /**
   * The association id to associate the LOF_SCORE of an object for the
   * LOF_SCORE algorithm.
   */
  public static final AssociationID<Double> LOF_SCORE = AssociationID.getOrCreateAssociationID("lof", Double.class);

  /**
   * Holds the instance of the reachability distance function specified by
   * {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}.
   */
  private DistanceFunction<O, D> reachabilityDistanceFunction;

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("lof.k", "The number of nearest neighbors of an object to be considered for computing its LOF_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOF_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -lof.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  int k;

  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * Preprocessor Step 1
   */
  MaterializeKNNPreprocessor<O, D> preprocessor1;

  /**
   * Preprocessor Step 2
   */
  MaterializeKNNPreprocessor<O, D> preprocessor2;
  
  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  boolean objectIsInKNN = false;

  /**
   * Provides the Generalized LOF_SCORE algorithm, adding parameters
   * {@link #K_PARAM} and {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM} to the
   * option handler additionally to parameters of super class.
   */
  public LOF() {
    super();
    // parameter k
    addOption(K_PARAM);
    // parameter reachability distance function
    addOption(REACHABILITY_DISTANCE_FUNCTION_PARAM);
    
    preprocessor1 = new MaterializeKNNPreprocessor<O, D>();
    preprocessor2 = new MaterializeKNNPreprocessor<O, D>();
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database.
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    reachabilityDistanceFunction.setDatabase(database, isVerbose(), isTime());

    // materialize neighborhoods
    HashMap<Integer, List<DistanceResultPair<D>>> neigh1;
    HashMap<Integer, List<DistanceResultPair<D>>> neigh2;
    if(logger.isVerbose()) {
      logger.verbose("Materializing Neighborhoods with respect to primary distance.");
    }
    preprocessor1.run(database, isVerbose(), isTime());
    neigh1 = preprocessor1.getMaterialized();
    if (getDistanceFunction() != reachabilityDistanceFunction) {
      if(logger.isVerbose()) {
        logger.verbose("Materializing Neighborhoods with respect to reachability distance.");
      }
      preprocessor2.run(database, isVerbose(), isTime());
      neigh2 = preprocessor2.getMaterialized();
    } else {
      if(logger.isVerbose()) {
        logger.verbose("Reusing neighborhoods of primary distance.");
      }
      neigh2 = neigh1;
    }

    HashMap<Integer, Double> lrds = new HashMap<Integer, Double>();
    {// computing LRDs
      if(logger.isVerbose()) {
        logger.verbose("Computing LRDs");
      }
      FiniteProgress lrdsProgress = new FiniteProgress("LRD", database.size());
      int counter = 0;
      for(Integer id : database) {
        counter ++;
        double sum = 0;
        List<DistanceResultPair<D>> neighbors = neigh2.get(id);
        int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
        for(DistanceResultPair<D> neighbor : neighbors) {
          if (objectIsInKNN || neighbor.getID() != id) {
            List<DistanceResultPair<D>> neighborsNeighbors = neigh2.get(neighbor.getID());
            sum += Math.max(neighbor.getDistance().getValue().doubleValue(), neighborsNeighbors.get(neighborsNeighbors.size() - 1).getDistance().getValue().doubleValue());
          }
        }
        Double lrd = nsize / sum;
        lrds.put(id, lrd);
        if(logger.isVerbose()) {
          lrdsProgress.setProcessed(counter);
          logger.progress(lrdsProgress);
        }
      }
    }
    // Compute final LOF values.
    HashMap<Integer, Double> lofs = new HashMap<Integer, Double>();
    // track the maximum value for normalization.
    MinMax<Double> lofminmax = new MinMax<Double>();
    {// compute LOF_SCORE of each db object
      if(logger.isVerbose()) {
        logger.verbose("computing LOFs");
      }

      FiniteProgress progressLOFs = new FiniteProgress("LOF_SCORE for objects", database.size());
      int counter = 0;
      for(Integer id : database) {
        counter ++;
        double lrdp = lrds.get(id);
        List<DistanceResultPair<D>> neighbors = neigh1.get(id);
        int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
        // skip the point itself
        //neighbors.remove(0);
        double sum = 0;
        for(DistanceResultPair<D> neighbor1 : neighbors) {
          if (objectIsInKNN || neighbor1.getID() != id) {
            double lrdo = lrds.get(neighbor1.getSecond());
            sum += lrdo / lrdp;
          }
        }
        Double lof = sum / nsize;
        lofs.put(id, lof);
        // update minimum and maximum
        lofminmax.put(lof);
        
        if(logger.isVerbose()) {
          progressLOFs.setProcessed(counter);
          logger.progress(progressLOFs);
        }
      }
    }

    if(logger.isVerbose()) {
      logger.verbose("LOF finished");
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(LOF_SCORE, lofs);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(lofs, true);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    this.result = new OutlierResult(scoreMeta, scoreResult, orderingResult);

    return result;
  }

  public Description getDescription() {
    return new Description(
        "LOF",
        "Local Outlier Factor",
        "Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter " +
            K_PARAM,
        "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander: " +
            " LOF: Identifying Density-Based Local Outliers. " +
            "In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000.");
  }

  /**
   * Calls the super method and sets additionally the value of the parameter
   * {@link #K_PARAM} and instantiates {@link #reachabilityDistanceFunction}
   * according to the value of parameter
   * {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}. The remaining parameters are
   * passed to the {@link #reachabilityDistanceFunction}.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // k
    k = K_PARAM.getValue();

    // reachabilityDistanceFunction - for parameter handling.
    if (REACHABILITY_DISTANCE_FUNCTION_PARAM.isSet()) {
      reachabilityDistanceFunction = REACHABILITY_DISTANCE_FUNCTION_PARAM.instantiateClass();
      addParameterizable(reachabilityDistanceFunction);
      remainingParameters = reachabilityDistanceFunction.setParameters(remainingParameters);
    } else {
      reachabilityDistanceFunction = getDistanceFunction();
    }
    
    // configure first preprocessor
    ArrayList<String> preprocParams1 = new ArrayList<String>();
    OptionUtil.addParameter(preprocParams1, MaterializeKNNPreprocessor.K_ID, Integer.toString(k+(objectIsInKNN ? 0 : 1)));
    OptionUtil.addParameter(preprocParams1, MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, getDistanceFunction().getClass().getCanonicalName());
    OptionUtil.addParameters(preprocParams1, getDistanceFunction().getParameters());
    List<String> remaining1 = preprocessor1.setParameters(preprocParams1);
    if (remaining1.size() > 0) {
      throw new UnusedParameterException("First preprocessor did not use all parameters.");
    }

    // configure second preprocessor
    ArrayList<String> preprocParams2 = new ArrayList<String>();
    OptionUtil.addParameter(preprocParams2, MaterializeKNNPreprocessor.K_ID, Integer.toString(k+(objectIsInKNN ? 0 : 1)));
    OptionUtil.addParameter(preprocParams2, MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, reachabilityDistanceFunction.getClass().getCanonicalName());
    OptionUtil.addParameters(preprocParams2, reachabilityDistanceFunction.getParameters());
    List<String> remaining2 = preprocessor2.setParameters(preprocParams2);
    if (remaining2.size() > 0) {
      throw new UnusedParameterException("Second preprocessor did not use all parameters.");
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  public MultiResult getResult() {
    return result;
  }
  
  
}
