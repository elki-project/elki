package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * LoOP: Local Outlier Probabilities
 * 
 * Distance/density based algorithm similar to LOF to detect outliers, but with
 * statistical methods to achieve better result stability.
 * 
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
@Title("LoOP: Local Outlier Probabilities")
@Description("Variant of the LOF algorithm normalized using statistical values.")
@Reference(authors = "Kriegel, H.-P., Kröger, P., Schubert, E., and Zimek, A.", title = "LoOP: Local Outlier Probabilities", booktitle = "Proceedings of the 18th International Conference on Information and Knowledge Management (CIKM), Hong Kong, China, 2009", url="http://dx.doi.org/10.1145/1645953.1646195")
public class LoOP<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * OptionID for {@link #REFERENCE_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID REFERENCE_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("loop.referencedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -loop.referencedistfunction}
   * </p>
   */
  private final ObjectParameter<DistanceFunction<O, DoubleDistance>> REFERENCE_DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, DoubleDistance>>(REFERENCE_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);

  /**
   * OptionID for {@link #COMPARISON_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID COMPARISON_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("loop.comparedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -loop.comparedistfunction}
   * </p>
   */
  private final ObjectParameter<DistanceFunction<O, DoubleDistance>> COMPARISON_DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, DoubleDistance>>(COMPARISON_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * OptionID for {@link #PREPROCESSOR_PARAM}
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("loop.preprocessor", "Preprocessor used to materialize the kNN neighborhoods.");

  /**
   * The preprocessor used to materialize the kNN neighborhoods.
   * 
   * Default value: {@link MaterializeKNNPreprocessor} </p>
   * <p>
   * Key: {@code -loop.preprocessor}
   * </p>
   */
  private final ClassParameter<MaterializeKNNPreprocessor<O, DoubleDistance>> PREPROCESSOR_PARAM = new ClassParameter<MaterializeKNNPreprocessor<O, DoubleDistance>>(PREPROCESSOR_ID, MaterializeKNNPreprocessor.class, MaterializeKNNPreprocessor.class);

  /**
   * The association id to associate the LOOP_SCORE of an object for the
   * LOOP_SCORE algorithm.
   */
  public static final AssociationID<Double> LOOP_SCORE = AssociationID.getOrCreateAssociationID("loop", Double.class);

  /**
   * OptionID for {@link #KCOMP_PARAM}
   */
  public static final OptionID KCOMP_ID = OptionID.getOrCreateOptionID("loop.kcomp", "The number of nearest neighbors of an object to be considered for computing its LOOP_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -loop.kcomp}
   * </p>
   */
  private final IntParameter KCOMP_PARAM = new IntParameter(KCOMP_ID, new GreaterConstraint(1));

  /**
   * OptionID for {@link #KCOMP_PARAM}
   */
  public static final OptionID KREF_ID = OptionID.getOrCreateOptionID("loop.kref", "The number of nearest neighbors of an object to be used for the PRD value.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -loop.kref}
   * </p>
   */
  private final IntParameter KREF_PARAM = new IntParameter(KREF_ID, new GreaterConstraint(1), true);

  /**
   * OptionID for {@link #LAMBDA_PARAM}
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("loop.lambda", "The number of standard deviations to consider for density computation.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -loop.lambda}
   * </p>
   */
  private final DoubleParameter LAMBDA_PARAM = new DoubleParameter(LAMBDA_ID, new GreaterConstraint(0.0), 2.0);

  /**
   * Holds the value of {@link #KCOMP_PARAM}.
   */
  int kcomp;

  /**
   * Holds the value of {@link #KREF_PARAM}.
   */
  int kref;

  /**
   * Hold the value of {@link #LAMBDA_PARAM}.
   */
  double lambda;

  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * Preprocessor Step 1
   */
  MaterializeKNNPreprocessor<O, DoubleDistance> preprocessorcompare;

  /**
   * Preprocessor Step 2
   */
  MaterializeKNNPreprocessor<O, DoubleDistance> preprocessorref;

  /**
   * Include object itself in kNN neighborhood.
   */
  boolean objectIsInKNN = false;

  /**
   * Provides the LoOP algorithm.
   */
  public LoOP(Parameterization config) {
    super(config);
    // Lambda
    if(config.grab(LAMBDA_PARAM)) {
      lambda = LAMBDA_PARAM.getValue();
    }

    // k
    if(config.grab(KCOMP_PARAM)) {
      kcomp = KCOMP_PARAM.getValue();
    }

    // k for reference set
    if(config.grab(KREF_PARAM)) {
      kref = KREF_PARAM.getValue();
    }
    else {
      kref = kcomp;
    }

    int preprock = kcomp;

    DistanceFunction<O, DoubleDistance> comparisonDistanceFunction = null;
    DistanceFunction<O, DoubleDistance> referenceDistanceFunction = null;

    if(config.grab(COMPARISON_DISTANCE_FUNCTION_PARAM)) {
      comparisonDistanceFunction = COMPARISON_DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }

    // referenceDistanceFunction
    if(config.grab(REFERENCE_DISTANCE_FUNCTION_PARAM)) {
      referenceDistanceFunction = REFERENCE_DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
    else {
      referenceDistanceFunction = null;
      // Adjust preprocessor k to accomodate both values
      preprock = Math.max(kcomp, kref);
    }

    // configure first preprocessor
    if(config.grab(PREPROCESSOR_PARAM) && COMPARISON_DISTANCE_FUNCTION_PARAM.isDefined()) {
      ListParameterization preprocParams1 = new ListParameterization();
      preprocParams1.addParameter(MaterializeKNNPreprocessor.K_ID, Integer.toString(preprock + (objectIsInKNN ? 0 : 1)));
      preprocParams1.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, comparisonDistanceFunction);
      ChainedParameterization chain = new ChainedParameterization(preprocParams1, config);
      // chain.errorsTo(config);
      preprocessorcompare = PREPROCESSOR_PARAM.instantiateClass(chain);
      chain.reportInternalParameterizationErrors(config);
      preprocParams1.reportInternalParameterizationErrors(config);

      // configure second preprocessor
      if(referenceDistanceFunction != null) {
        ListParameterization preprocParams2 = new ListParameterization();
        preprocParams2.addParameter(MaterializeKNNPreprocessor.K_ID, Integer.toString(kcomp + (objectIsInKNN ? 0 : 1)));
        preprocParams2.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, referenceDistanceFunction);
        ChainedParameterization chain2 = new ChainedParameterization(preprocParams2, config);
        // chain2.errorsTo(config);
        preprocessorref = PREPROCESSOR_PARAM.instantiateClass(chain2);
        chain2.reportInternalParameterizationErrors(config);
        preprocParams2.reportInternalParameterizationErrors(config);
      }
    }
  }

  /**
   * Performs the LoOP algorithm on the given database.
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    final double sqrt2 = Math.sqrt(2.0);

    // materialize neighborhoods
    HashMap<Integer, List<DistanceResultPair<DoubleDistance>>> neighcompare;
    HashMap<Integer, List<DistanceResultPair<DoubleDistance>>> neighref;

    preprocessorcompare.run(database, isVerbose(), isTime());
    neighcompare = preprocessorcompare.getMaterialized();
    if(logger.isVerbose()) {
      logger.verbose("Materializing neighborhoods with respect to reachability distance.");
    }
    if(REFERENCE_DISTANCE_FUNCTION_PARAM.isDefined()) {
      if(logger.isVerbose()) {
        logger.verbose("Materializing neighborhoods for (separate) reference set function.");
      }
      preprocessorref.run(database, isVerbose(), isTime());
      neighref = preprocessorref.getMaterialized();
    }
    else {
      neighref = neighcompare;
    }

    // Probabilistic distances
    HashMap<Integer, Double> pdists = new HashMap<Integer, Double>();
    {// computing PRDs
      if(logger.isVerbose()) {
        logger.verbose("Computing pdists");
      }
      FiniteProgress prdsProgress = new FiniteProgress("pdists", database.size());
      int counter = 0;
      for(Integer id : database) {
        counter++;
        List<DistanceResultPair<DoubleDistance>> neighbors = neighref.get(id);
        double sqsum = 0.0;
        // use first kref neighbors as reference set
        int ks = 0;
        for(DistanceResultPair<DoubleDistance> neighbor : neighbors) {
          if(objectIsInKNN || neighbor.getID() != id) {
            double d = neighbor.getDistance().doubleValue();
            sqsum += d * d;
            ks++;
            if(ks >= kref) {
              break;
            }
          }
        }
        Double pdist = lambda * Math.sqrt(sqsum / ks);
        pdists.put(id, pdist);
        if(logger.isVerbose()) {
          prdsProgress.setProcessed(counter);
          logger.progress(prdsProgress);
        }
      }
    }
    // Compute PLOF values.
    HashMap<Integer, Double> plofs = new HashMap<Integer, Double>();
    MeanVariance mvplof = new MeanVariance();
    {// compute LOOP_SCORE of each db object
      if(logger.isVerbose()) {
        logger.verbose("Computing PLOF");
      }

      FiniteProgress progressPLOFs = new FiniteProgress("PLOFs for objects", database.size());
      int counter = 0;
      for(Integer id : database) {
        counter++;
        List<DistanceResultPair<DoubleDistance>> neighbors = neighcompare.get(id);
        MeanVariance mv = new MeanVariance();
        // use first kref neighbors as comparison set.
        int ks = 0;
        for(DistanceResultPair<DoubleDistance> neighbor1 : neighbors) {
          if(objectIsInKNN || neighbor1.getID() != id) {
            mv.put(pdists.get(neighbor1.getSecond()));
            ks++;
            if(ks >= kcomp) {
              break;
            }
          }
        }
        double plof = Math.max(pdists.get(id) / mv.getMean(), 1.0);
        plofs.put(id, plof);
        mvplof.put((plof - 1.0) * (plof - 1.0));

        if(logger.isVerbose()) {
          progressPLOFs.setProcessed(counter);
          logger.progress(progressPLOFs);
        }
      }
    }

    double nplof = lambda * Math.sqrt(mvplof.getMean());
    if(logger.isVerbose()) {
      logger.verbose("nplof normalization factor is " + nplof + " " + mvplof.getMean() + " " + mvplof.getStddev());
    }

    // Compute final LoOP values.
    HashMap<Integer, Double> loops = new HashMap<Integer, Double>();
    {// compute LOOP_SCORE of each db object
      if(logger.isVerbose()) {
        logger.verbose("Computing LoOP");
      }

      FiniteProgress progressLOOPs = new FiniteProgress("LoOP for objects", database.size());
      int counter = 0;
      for(Integer id : database) {
        counter++;
        List<DistanceResultPair<DoubleDistance>> neighbors = neighcompare.get(id);
        MeanVariance mv = new MeanVariance();
        // use first kref neighbors as comparison set.
        int ks = 0;
        for(DistanceResultPair<DoubleDistance> neighbor1 : neighbors) {
          if(objectIsInKNN || neighbor1.getID() != id) {
            mv.put(pdists.get(neighbor1.getSecond()));
            ks++;
            if(ks >= kcomp) {
              break;
            }
          }
        }
        double plof = Math.max(pdists.get(id) / mv.getMean(), 1.0);
        loops.put(id, ErrorFunctions.erf((plof - 1) / (nplof * sqrt2)));

        if(logger.isVerbose()) {
          progressLOOPs.setProcessed(counter);
          logger.progress(progressLOOPs);
        }
      }
    }

    if(logger.isVerbose()) {
      logger.verbose("LoOP finished");
    }

    // Build result representation.
    result = new MultiResult();
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(LOOP_SCORE, loops);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(loops, true);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    this.result = new OutlierResult(scoreMeta, scoreResult, orderingResult);

    return this.result;
  }

  public MultiResult getResult() {
    return result;
  }
}