package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQueryFactory;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQueryFactory;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
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
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "LoOP: Local Outlier Probabilities", booktitle = "Proceedings of the 18th International Conference on Information and Knowledge Management (CIKM), Hong Kong, China, 2009", url = "http://dx.doi.org/10.1145/1645953.1646195")
public class LoOP<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LoOP.class);
  
  /**
   * The association id to associate the LOOP_SCORE of an object for the
   * LOOP_SCORE algorithm.
   */
  public static final AssociationID<Double> LOOP_SCORE = AssociationID.getOrCreateAssociationID("loop", Double.class);

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID REFERENCE_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("loop.referencedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID COMPARISON_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("loop.comparedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   */
  public static final OptionID KCOMP_ID = OptionID.getOrCreateOptionID("loop.kcomp", "The number of nearest neighbors of an object to be considered for computing its LOOP_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   */
  public static final OptionID KREF_ID = OptionID.getOrCreateOptionID("loop.kref", "The number of nearest neighbors of an object to be used for the PRD value.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("loop.lambda", "The number of standard deviations to consider for density computation.");

  /**
   * Holds the value of {@link #KCOMP_ID}.
   */
  int kcomp;

  /**
   * Holds the value of {@link #KREF_ID}.
   */
  int kref;

  /**
   * Hold the value of {@link #LAMBDA_ID}.
   */
  double lambda;

  /**
   * Preprocessor Step 1
   */
  protected KNNQueryFactory<O, D> knnQueryCompare;

  /**
   * Preprocessor Step 2
   */
  protected KNNQueryFactory<O, D> knnQueryReference;

  /**
   * Include object itself in kNN neighborhood.
   */
  static boolean objectIsInKNN = false;

  /**
   * Constructor with parameters.
   * 
   * @param kcomp
   * @param kref
   * @param knnQueryCompare
   * @param knnQueryReference
   * @param lambda
   */
  public LoOP(int kcomp, int kref, KNNQueryFactory<O, D> knnQueryCompare, KNNQueryFactory<O, D> knnQueryReference, double lambda) {
    super();
    this.kcomp = kcomp;
    this.kref = kref;
    this.knnQueryCompare = knnQueryCompare;
    this.knnQueryReference = knnQueryReference;
    this.lambda = lambda;
  }

  /**
   * Performs the LoOP algorithm on the given database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    final double sqrt2 = Math.sqrt(2.0);

    StepProgress stepprog = logger.isVerbose() ? new StepProgress(5) : null;

    // neighborhoods queries
    KNNQuery<O, D> neighcompare;
    KNNQuery<O, D> neighref;

    neighcompare = knnQueryCompare.instantiate(database);
    if(stepprog != null) {
      stepprog.beginStep(1, "Materializing neighborhoods with respect to reachability distance.", logger);
    }
    if(knnQueryReference != knnQueryCompare) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Materializing neighborhoods for (separate) reference set function.", logger);
      }
      neighref = knnQueryReference.instantiate(database);
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(2, "Re-using the neighborhoods.", logger);
      }
      neighref = neighcompare;
    }

    // Probabilistic distances
    WritableDataStore<Double> pdists = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    {// computing PRDs
      if(stepprog != null) {
        stepprog.beginStep(3, "Computing pdists", logger);
      }
      FiniteProgress prdsProgress = logger.isVerbose() ? new FiniteProgress("pdists", database.size(), logger) : null;
      for(DBID id : database) {
        List<DistanceResultPair<D>> neighbors = neighref.getKNNForDBID(id, kref);
        double sqsum = 0.0;
        // use first kref neighbors as reference set
        int ks = 0;
        for(DistanceResultPair<D> neighbor : neighbors) {
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
        if(prdsProgress != null) {
          prdsProgress.incrementProcessed(logger);
        }
      }
    }
    // Compute PLOF values.
    WritableDataStore<Double> plofs = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    MeanVariance mvplof = new MeanVariance();
    {// compute LOOP_SCORE of each db object
      if(stepprog != null) {
        stepprog.beginStep(4, "Computing PLOF", logger);
      }

      FiniteProgress progressPLOFs = logger.isVerbose() ? new FiniteProgress("PLOFs for objects", database.size(), logger) : null;
      for(DBID id : database) {
        List<DistanceResultPair<D>> neighbors = neighcompare.getKNNForDBID(id, kcomp);
        MeanVariance mv = new MeanVariance();
        // use first kref neighbors as comparison set.
        int ks = 0;
        for(DistanceResultPair<D> neighbor1 : neighbors) {
          if(objectIsInKNN || neighbor1.getID() != id) {
            mv.put(pdists.get(neighbor1.getSecond()));
            ks++;
            if(ks >= kcomp) {
              break;
            }
          }
        }
        double plof = Math.max(pdists.get(id) / mv.getMean(), 1.0);
        if(Double.isNaN(plof) || Double.isInfinite(plof)) {
          plof = 1.0;
        }
        plofs.put(id, plof);
        mvplof.put((plof - 1.0) * (plof - 1.0));

        if(progressPLOFs != null) {
          progressPLOFs.incrementProcessed(logger);
        }
      }
    }

    double nplof = lambda * Math.sqrt(mvplof.getMean());
    if(logger.isDebugging()) {
      logger.verbose("nplof normalization factor is " + nplof + " " + mvplof.getMean() + " " + mvplof.getStddev());
    }

    // Compute final LoOP values.
    WritableDataStore<Double> loops = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    {// compute LOOP_SCORE of each db object
      if(stepprog != null) {
        stepprog.beginStep(5, "Computing LoOP scores", logger);
      }

      FiniteProgress progressLOOPs = logger.isVerbose() ? new FiniteProgress("LoOP for objects", database.size(), logger) : null;
      for(DBID id : database) {
        loops.put(id, ErrorFunctions.erf((plofs.get(id) - 1) / (nplof * sqrt2)));

        if(progressLOOPs != null) {
          progressLOOPs.incrementProcessed(logger);
        }
      }
    }

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Local Outlier Probabilities", "loop-outlier", LOOP_SCORE, loops);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> LoOP<O, D> parameterize(Parameterization config) {
    int kcomp = getParameterKcomp(config);
    int kref = getParameterKref(config, kcomp);
    DistanceFunction<O, D> comparisonDistanceFunction = getParameterComparisonDistanceFunction(config);
    DistanceFunction<O, D> referenceDistanceFunction = getParameterReferenceDistanceFunction(config);
    final KNNQueryFactory<O, D> knnQuery1;
    final KNNQueryFactory<O, D> knnQuery2;
    if (referenceDistanceFunction == null) {
      int kmax = Math.max(kcomp, kref);
      knnQuery1 = getParameterKNNQuery(config, kmax + (objectIsInKNN ? 0 : 1), comparisonDistanceFunction, PreprocessorKNNQueryFactory.class);
      knnQuery2 = knnQuery1;
      referenceDistanceFunction = comparisonDistanceFunction;
    } else {
      knnQuery1 = getParameterKNNQuery(config, kcomp + (objectIsInKNN ? 0 : 1), comparisonDistanceFunction, PreprocessorKNNQueryFactory.class);
      knnQuery2 = getParameterKNNQuery(config, kref + (objectIsInKNN ? 0 : 1), referenceDistanceFunction, PreprocessorKNNQueryFactory.class);
    }
    double lambda = getParameterLambda(config);
    if(config.hasErrors()) {
      return null;
    }
    return new LoOP<O, D>(kcomp, kref, knnQuery1, knnQuery2, lambda);
  }

  /**
   * Get the lambda parameter
   * 
   * @param config Parameterization
   * @return lambda parameter
   */
  protected static double getParameterLambda(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(LAMBDA_ID, new GreaterConstraint(0.0), 2.0);
    if(config.grab(param)) {
      return param.getValue();
    }
    return Double.NaN;
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterKcomp(Parameterization config) {
    final IntParameter param = new IntParameter(KCOMP_ID, new GreaterConstraint(1));
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @param kcomp Fallback value
   * @return k parameter
   */
  protected static int getParameterKref(Parameterization config, int kcomp) {
    final IntParameter param = new IntParameter(KREF_ID, new GreaterConstraint(1), true);
    if(config.grab(param)) {
      return param.getValue();
    }
    return kcomp;
  }

  /**
   * Grab the comparison distance configuration option.
   * 
   * @param <F> distance function type
   * @param config Parameterization
   * @return Parameter value or null.
   */
  protected static <F extends DistanceFunction<?, ?>> F getParameterComparisonDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(COMPARISON_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  /**
   * Grab the reference distance configuration option.
   * 
   * @param <F> distance function type
   * @param config Parameterization
   * @return Parameter value or null.
   */
  protected static <F extends DistanceFunction<?, ?>> F getParameterReferenceDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(REFERENCE_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}