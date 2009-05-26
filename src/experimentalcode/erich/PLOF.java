package experimentalcode.erich;

import java.util.ArrayList;
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
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * Algorithm to compute density-based <em>probabilistic</em> local outlier
 * factors in a database based on a specified parameter {@link #KCOMP_ID}.
 * 
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class PLOF<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * OptionID for {@link #REFERENCE_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID REFERENCE_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("plof.referencedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -plof.referencedistfunction}
   * </p>
   */
  private final ClassParameter<DistanceFunction<O, DoubleDistance>> REFERENCE_DISTANCE_FUNCTION_PARAM = new ClassParameter<DistanceFunction<O, DoubleDistance>>(REFERENCE_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);

  /**
   * OptionID for {@link #COMPARISON_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID COMPARISON_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("plof.comparedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -plof.comparedistfunction}
   * </p>
   */
  private final ClassParameter<DistanceFunction<O, DoubleDistance>> COMPARISON_DISTANCE_FUNCTION_PARAM = new ClassParameter<DistanceFunction<O, DoubleDistance>>(COMPARISON_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class.getCanonicalName());

  /**
   * The association id to associate the LOOP_SCORE of an object for the
   * LOOP_SCORE algorithm.
   */
  public static final AssociationID<Double> LOOP_SCORE = AssociationID.getOrCreateAssociationID("plof", Double.class);

  /**
   * OptionID for {@link #KCOMP_PARAM}
   */
  public static final OptionID KCOMP_ID = OptionID.getOrCreateOptionID("plof.kcomp", "The number of nearest neighbors of an object to be considered for computing its LOOP_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOF_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -plof.k}
   * </p>
   */
  private final IntParameter KCOMP_PARAM = new IntParameter(KCOMP_ID, new GreaterConstraint(1));

  /**
   * OptionID for {@link #KCOMP_PARAM}
   */
  public static final OptionID KREF_ID = OptionID.getOrCreateOptionID("plof.kref", "The number of nearest neighbors of an object to be used for the PRD value.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOF_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -plof.k2}
   * </p>
   */
  private final IntParameter KREF_PARAM = new IntParameter(KREF_ID, new GreaterConstraint(1), true);

  /**
   * OptionID for {@link #LAMBDA_PARAM}
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("plof.lambda", "The number of standard deviations to consider for density computation.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOF_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -plof.lambda}
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
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  boolean objectIsInKNN = false;

  /**
   * Provides the Generalized LOF_SCORE algorithm, adding parameters
   * {@link #KCOMP_PARAM} and {@link #REFERENCE_DISTANCE_FUNCTION_PARAM} to the
   * option handler additionally to parameters of super class.
   */
  public PLOF() {
    super();
    addOption(KCOMP_PARAM);
    addOption(KREF_PARAM);
    addOption(COMPARISON_DISTANCE_FUNCTION_PARAM);
    addOption(REFERENCE_DISTANCE_FUNCTION_PARAM);
    addOption(LAMBDA_PARAM);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database.
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
    if(REFERENCE_DISTANCE_FUNCTION_PARAM.isSet()) {
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
    MeanVariance pdmean = new MeanVariance();
    {// computing PRDs
      if(logger.isVerbose()) {
        logger.verbose("Computing pdists");
      }
      FiniteProgress prdsProgress = new FiniteProgress("pdists", database.size());
      int counter = 0;
      for(Integer id : database) {
        counter++;
        List<DistanceResultPair<DoubleDistance>> neighbors = neighref.get(id);
        MeanVariance mv = new MeanVariance();
        // use first kref neighbors as reference set
        int ks = 0;
        for(DistanceResultPair<DoubleDistance> neighbor : neighbors) {
          if(objectIsInKNN || neighbor.getID() != id) {
            mv.put(neighbor.getDistance().getValue());
            ks++;
            if(ks >= kref) {
              break;
            }
          }
        }
        Double pdist = (mv.getMean() + lambda * mv.getStddev());
        pdists.put(id, pdist);
        pdmean.put(pdist);
        if(logger.isVerbose()) {
          prdsProgress.setProcessed(counter);
          logger.progress(prdsProgress);
        }
      }
    }
    double nplof = ((pdmean.getMean() + lambda * pdmean.getStddev()) / pdmean.getMean()) - 1;
    if (logger.isVerbose()) {
      logger.verbose("nplof normalization factor is "+nplof);
    }
    // Compute final PLOF values.
    HashMap<Integer, Double> loops = new HashMap<Integer, Double>();
    {// compute LOOP_SCORE of each db object
      if(logger.isVerbose()) {
        logger.verbose("Computing LoOPs");
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
    result.addResult(new AnnotationFromHashMap<Double>(LOOP_SCORE, loops));
    result.addResult(new OrderingFromHashMap<Double>(loops, true));

    return result;
  }

  public Description getDescription() {
    return new Description("LooP", "Local Outlier Probability", "Variant of the LOF algorithm normalized using statistical values.", "unpublished");
  }

  /**
   * Calls the super method and sets additionally the value of the parameter
   * {@link #KCOMP_PARAM} and instantiates {@link #referenceDistanceFunction}
   * according to the value of parameter
   * {@link #REFERENCE_DISTANCE_FUNCTION_PARAM}. The remaining parameters are
   * passed to the {@link #referenceDistanceFunction}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // k
    kcomp = KCOMP_PARAM.getValue();

    // k for reference set
    if(KREF_PARAM.isSet()) {
      kref = KREF_PARAM.getValue();
    }
    else {
      kref = kcomp;
    }

    int preprock = kcomp;
    
    DistanceFunction<O, DoubleDistance> comparisonDistanceFunction;
    DistanceFunction<O, DoubleDistance> referenceDistanceFunction;
    
    comparisonDistanceFunction = COMPARISON_DISTANCE_FUNCTION_PARAM.instantiateClass();
    remainingParameters = comparisonDistanceFunction.setParameters(remainingParameters);
    addParameterizable(comparisonDistanceFunction);
    
    // referenceDistanceFunction
    if(REFERENCE_DISTANCE_FUNCTION_PARAM.isSet()) {
      referenceDistanceFunction = REFERENCE_DISTANCE_FUNCTION_PARAM.instantiateClass();
      remainingParameters = referenceDistanceFunction.setParameters(remainingParameters);
      addParameterizable(referenceDistanceFunction);
    }
    else {
      referenceDistanceFunction = null;
      // Adjust preprocessor k to accomodate both values
      preprock = Math.max(kcomp, kref);
    }

    // configure first preprocessor
    preprocessorcompare = new MaterializeKNNPreprocessor<O, DoubleDistance>();
    List<String> preprocParams1 = new ArrayList<String>();
    OptionUtil.addParameter(preprocParams1, MaterializeKNNPreprocessor.K_ID, Integer.toString(preprock + (objectIsInKNN ? 0 : 1)));
    OptionUtil.addParameter(preprocParams1, MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, comparisonDistanceFunction.getClass().getCanonicalName());
    OptionUtil.addParameters(preprocParams1, comparisonDistanceFunction.getParameters());
    String[] remaining1 = preprocessorcompare.setParameters(ClassGenericsUtil.toArray(preprocParams1, String.class));
    if(remaining1.length > 0) {
      throw new UnusedParameterException("First preprocessor did not use all parameters.");
    }

    // configure second preprocessor
    if(referenceDistanceFunction != null) {
      preprocessorref = new MaterializeKNNPreprocessor<O, DoubleDistance>();
      List<String> preprocParams2 = new ArrayList<String>();
      OptionUtil.addParameter(preprocParams2, MaterializeKNNPreprocessor.K_ID, Integer.toString(kcomp + (objectIsInKNN ? 0 : 1)));
      OptionUtil.addParameter(preprocParams2, MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, referenceDistanceFunction.getClass().getCanonicalName());
      OptionUtil.addParameters(preprocParams2, referenceDistanceFunction.getParameters());
      String[] remaining2 = preprocessorref.setParameters(ClassGenericsUtil.toArray(preprocParams2, String.class));
      if(remaining2.length > 0) {
        throw new UnusedParameterException("Second preprocessor did not use all parameters.");
      }
    }
    
    lambda = LAMBDA_PARAM.getValue();

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Calls the super method and appends the parameter description of
   * {@link #referenceDistanceFunction} (if it is already initialized).
   */
  @Override
  public String parameterDescription() {
    StringBuilder description = new StringBuilder();
    description.append(super.parameterDescription());

    return description.toString();
  }

  public MultiResult getResult() {
    return result;
  }
}
