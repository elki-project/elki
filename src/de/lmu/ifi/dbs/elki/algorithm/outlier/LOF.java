package de.lmu.ifi.dbs.elki.algorithm.outlier;

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
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter {@link #K_ID} ({@code -lof.k}).
 * </p>
 * 
 * <p>
 * This implementation diverts from the original LOF publication in that it
 * allows the user to use a different distance function for the reachability
 * distance and neighborhood determination (although the default is to use the
 * same value.)
 * </p>
 * 
 * <p>
 * The k nearest neighbors are determined using the parameter
 * {@link DistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}, while the reference set
 * used in reachability distance computation is configured using
 * {@link #REACHABILITY_DISTANCE_FUNCTION_ID}.
 * </p>
 * 
 * <p>
 * The original LOF parameter was called &quot;minPts&quot;. Since kNN queries
 * in ELKI have slightly different semantics - exactly k neighbors are returned
 * - we chose to rename the parameter to {@link #K_ID} ({@code -lof.k}) to
 * reflect this difference.
 * </p>
 * 
 * <p>
 * Reference: <br>
 * M. M. Breunig, H.-P. Kriegel, R. Ng, J. Sander: LOF: Identifying
 * Density-Based Local Outliers. <br>
 * In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00),
 * Dallas, TX, 2000.
 * </p>
 * 
 * @author Peer Kröger
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "LOF: Identifying Density-Based Local Outliers", booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", url="http://dx.doi.org/10.1145/342009.335388")
public class LOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends DistanceBasedAlgorithm<O, D, OutlierResult> {
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
  private final ObjectParameter<DistanceFunction<O, D>> REACHABILITY_DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);

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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LOF(Parameterization config) {
    super(config);
    // parameter k
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    // parameter reachability distance function
    if(config.grab(REACHABILITY_DISTANCE_FUNCTION_PARAM)) {
      reachabilityDistanceFunction = REACHABILITY_DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
    else {
      reachabilityDistanceFunction = getDistanceFunction();
    }

    // configure first preprocessor
    ListParameterization preprocParams1 = new ListParameterization();
    preprocParams1.addParameter(MaterializeKNNPreprocessor.K_ID, Integer.toString(k + (objectIsInKNN ? 0 : 1)));
    preprocParams1.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, getDistanceFunction());
    ChainedParameterization chain = new ChainedParameterization(preprocParams1, config);
    chain.errorsTo(config);
    preprocessor1 = new MaterializeKNNPreprocessor<O, D>(chain);
    preprocParams1.reportInternalParameterizationErrors(config);

    // TODO: reuse the previous preprocessor if we're using the same distance!
    // configure second preprocessor
    ListParameterization preprocParams2 = new ListParameterization();
    preprocParams2.addParameter(MaterializeKNNPreprocessor.K_ID, Integer.toString(k + (objectIsInKNN ? 0 : 1)));
    preprocParams2.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, reachabilityDistanceFunction);
    ChainedParameterization chain2 = new ChainedParameterization(preprocParams2, config);
    chain2.errorsTo(config);
    preprocessor2 = new MaterializeKNNPreprocessor<O, D>(chain2);
    preprocParams2.reportInternalParameterizationErrors(config);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database);
    reachabilityDistanceFunction.setDatabase(database);

    // materialize neighborhoods
    HashMap<Integer, List<DistanceResultPair<D>>> neigh1;
    HashMap<Integer, List<DistanceResultPair<D>>> neigh2;
    if(logger.isVerbose()) {
      logger.verbose("Materializing Neighborhoods with respect to primary distance.");
    }
    preprocessor1.run(database, isVerbose(), isTime());
    neigh1 = preprocessor1.getMaterialized();
    if(getDistanceFunction() != reachabilityDistanceFunction) {
      if(logger.isVerbose()) {
        logger.verbose("Materializing Neighborhoods with respect to reachability distance.");
      }
      preprocessor2.run(database, isVerbose(), isTime());
      neigh2 = preprocessor2.getMaterialized();
    }
    else {
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
        counter++;
        double sum = 0;
        List<DistanceResultPair<D>> neighbors = neigh2.get(id);
        int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
        for(DistanceResultPair<D> neighbor : neighbors) {
          if(objectIsInKNN || neighbor.getID() != id) {
            List<DistanceResultPair<D>> neighborsNeighbors = neigh2.get(neighbor.getID());
            sum += Math.max(neighbor.getDistance().doubleValue(), neighborsNeighbors.get(neighborsNeighbors.size() - 1).getDistance().doubleValue());
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
        counter++;
        double lrdp = lrds.get(id);
        List<DistanceResultPair<D>> neighbors = neigh1.get(id);
        int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
        // skip the point itself
        // neighbors.remove(0);
        double sum = 0;
        for(DistanceResultPair<D> neighbor1 : neighbors) {
          if(objectIsInKNN || neighbor1.getID() != id) {
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
    return new OutlierResult(scoreMeta, scoreResult, orderingResult);
  }
}