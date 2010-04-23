package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
 * @author Peer Kr&ouml;ger
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "LOF: Identifying Density-Based Local Outliers", booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", url = "http://dx.doi.org/10.1145/342009.335388")
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
  protected DistanceFunction<O, D> reachabilityDistanceFunction;

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
   * OptionID for {@link #PREPROCESSOR_PARAM}
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("lof.preprocessor", "Preprocessor used to materialize the kNN neighborhoods.");

  /**
   * The preprocessor used to materialize the kNN neighborhoods.
   * 
   * Default value: {@link MaterializeKNNPreprocessor} </p>
   * <p>
   * Key: {@code -loop.preprocessor}
   * </p>
   */
  private final ClassParameter<MaterializeKNNPreprocessor<O, D>> PREPROCESSOR_PARAM = new ClassParameter<MaterializeKNNPreprocessor<O, D>>(PREPROCESSOR_ID, MaterializeKNNPreprocessor.class, MaterializeKNNPreprocessor.class);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  protected int k;

  /**
   * Preprocessor Step 1
   */
  protected MaterializeKNNPreprocessor<O, D> preprocessor1;

  /**
   * Preprocessor Step 2
   */
  protected MaterializeKNNPreprocessor<O, D> preprocessor2;

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
    if(config.grab(PREPROCESSOR_PARAM) && DISTANCE_FUNCTION_PARAM.isDefined()) {
      ListParameterization preprocParams1 = new ListParameterization();
      preprocParams1.addParameter(MaterializeKNNPreprocessor.K_ID, Integer.toString(k + (objectIsInKNN ? 0 : 1)));
      preprocParams1.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, getDistanceFunction());
      ChainedParameterization chain = new ChainedParameterization(preprocParams1, config);
      chain.errorsTo(config);
      preprocessor1 = PREPROCESSOR_PARAM.instantiateClass(chain);
      preprocParams1.reportInternalParameterizationErrors(config);

      if(reachabilityDistanceFunction != null) {
        // configure second preprocessor
        ListParameterization preprocParams2 = new ListParameterization();
        preprocParams2.addParameter(MaterializeKNNPreprocessor.K_ID, Integer.toString(k + (objectIsInKNN ? 0 : 1)));
        preprocParams2.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, reachabilityDistanceFunction);
        ChainedParameterization chain2 = new ChainedParameterization(preprocParams2, config);
        chain2.errorsTo(config);
        preprocessor2 = PREPROCESSOR_PARAM.instantiateClass(chain2);
        preprocParams2.reportInternalParameterizationErrors(config);
      }
    }
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)}.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    return doRunInTime(database).getResult();
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database and
   * returns a {@link LOFResult} encapsulating information that may be needed by
   * an {@link OnlineLOF} algorithm.
   */
  protected LOFResult doRunInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database);
    reachabilityDistanceFunction.setDatabase(database);

    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    // materialize neighborhoods
    HashMap<Integer, List<DistanceResultPair<D>>> neigh1;
    HashMap<Integer, List<DistanceResultPair<D>>> neigh2;
    if(stepprog != null) {
      stepprog.beginStep(1, "Materializing Neighborhoods with respect to primary distance.", logger);
    }
    preprocessor1.run(database);
    neigh1 = preprocessor1.getMaterialized();
    if(getDistanceFunction() != reachabilityDistanceFunction) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Materializing Neighborhoods with respect to reachability distance.", logger);
      }
      preprocessor2.run(database);
      neigh2 = preprocessor2.getMaterialized();
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(2, "Reusing neighborhoods of primary distance.", logger);
      }
      neigh2 = neigh1;
    }

    // Compute LRDs
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing LRDs", logger);
    }
    HashMap<Integer, Double> lrds = computeLRDs(database.getIDs(), neigh2);

    // compute LOF_SCORE of each db object
    if(stepprog != null) {
      stepprog.beginStep(4, "computing LOFs", logger);
    }
    Pair<HashMap<Integer, Double>, MinMax<Double>> lofsAndMax = computeLOFs(database.getIDs(), lrds, neigh1);
    HashMap<Integer, Double> lofs = lofsAndMax.getFirst();
    // track the maximum value for normalization.
    MinMax<Double> lofminmax = lofsAndMax.getSecond();

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(LOF_SCORE, lofs);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(lofs, true);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult, orderingResult);

    return new LOFResult(result, neigh1, neigh2, lrds, lofs);
  }

  /**
   * Computes the local reachability density (LRD) of the specified objects.
   * 
   * @param ids the ids of the objects
   * @param neigh2 the precomputed neighborhood of the objects w.r.t. the
   *        reachability distance
   * @return the LRDs of the objects
   */
  protected HashMap<Integer, Double> computeLRDs(List<Integer> ids, HashMap<Integer, List<DistanceResultPair<D>>> neigh2) {
    HashMap<Integer, Double> lrds = new HashMap<Integer, Double>();
    FiniteProgress lrdsProgress = logger.isVerbose() ? new FiniteProgress("LRD", ids.size(), logger) : null;
    int counter = 0;
    for(Integer id : ids) {
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
      if(lrdsProgress != null) {
        lrdsProgress.setProcessed(counter, logger);
      }
    }
    if(lrdsProgress != null) {
      lrdsProgress.ensureCompleted(logger);
    }
    return lrds;
  }

  /**
   * Computes the Local outlier factor (LOF) of the specified objects.
   * 
   * @param ids the ids of the objects
   * @param lrds the LRDs of the objects
   * @param neigh1 the precomputed neighborhood of the objects w.r.t. the
   *        primary distance
   * @return the LOFs of the objects and the maximum LOF
   */
  protected Pair<HashMap<Integer, Double>, MinMax<Double>> computeLOFs(List<Integer> ids, HashMap<Integer, Double> lrds, HashMap<Integer, List<DistanceResultPair<D>>> neigh1) {
    HashMap<Integer, Double> lofs = new HashMap<Integer, Double>();
    // track the maximum value for normalization.
    MinMax<Double> lofminmax = new MinMax<Double>();

    FiniteProgress progressLOFs = logger.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), logger) : null;
    int counter = 0;
    for(Integer id : ids) {
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

      if(progressLOFs != null) {
        progressLOFs.setProcessed(counter, logger);
      }
    }
    if(progressLOFs != null) {
      progressLOFs.ensureCompleted(logger);
    }
    return new Pair<HashMap<Integer, Double>, MinMax<Double>>(lofs, lofminmax);
  }

  /**
   * Encapsulates information like the neighborhood, the LRD and LOF values of
   * the objects during a run of the {@link LOF} algorithm.
   */
  public class LOFResult {
    /**
     * The result of the run of the {@link LOF} algorithm.
     */
    private OutlierResult result;

    /**
     * The neighborhood of the objects w.r.t. the primary distance.
     */
    private HashMap<Integer, List<DistanceResultPair<D>>> neigh1;

    /**
     * The neighborhood of the objects w.r.t. the reachability distance.
     */
    private HashMap<Integer, List<DistanceResultPair<D>>> neigh2;

    /**
     * The LRD values of the objects.
     */
    private HashMap<Integer, Double> lrds;

    /**
     * The LOF values of the objects.
     */
    private HashMap<Integer, Double> lofs;

    /**
     * Encapsulates information generated during a run of the {@link LOF}
     * algorithm.
     * 
     * @param result the result of the run of the {@link LOF} algorithm
     * @param neigh1 the neighborhood of the objects w.r.t. the primary distance
     * @param neigh2 the neighborhood of the objects w.r.t. the reachability
     *        distance
     * @param lrds the LRD values of the objects
     * @param lofs the LOF values of the objects
     */
    public LOFResult(OutlierResult result, HashMap<Integer, List<DistanceResultPair<D>>> neigh1, HashMap<Integer, List<DistanceResultPair<D>>> neigh2, HashMap<Integer, Double> lrds, HashMap<Integer, Double> lofs) {
      this.result = result;
      this.neigh1 = neigh1;
      this.neigh2 = neigh2;
      this.lrds = lrds;
      this.lofs = lofs;
    }

    /**
     * @return the neighborhood of the objects w.r.t. the primary distance
     */
    public HashMap<Integer, List<DistanceResultPair<D>>> getNeigh1() {
      return neigh1;
    }

    /**
     * @return the neighborhood of the objects w.r.t. the reachability distance
     */
    public HashMap<Integer, List<DistanceResultPair<D>>> getNeigh2() {
      return neigh2;
    }

    /**
     * @return the LRD values of the objects
     */
    public HashMap<Integer, Double> getLrds() {
      return lrds;
    }

    /**
     * @return the LOF values of the objects
     */
    public HashMap<Integer, Double> getLofs() {
      return lofs;
    }

    /**
     * @return the result of the run of the {@link LOF} algorithm
     */
    public OutlierResult getResult() {
      return result;
    }
  }
}