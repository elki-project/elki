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
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.MaterializeKNNPreprocessor;
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
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("loop.referencedistfunction", "Distance function to determine the density of an object.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID COMPARISON_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("loop.comparedistfunction", "Distance function to determine the reference set of an object.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   */
  public static final OptionID KREACH_ID = OptionID.getOrCreateOptionID("loop.kref", "The number of nearest neighbors of an object to be used for the PRD value.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   */
  public static final OptionID KCOMP_ID = OptionID.getOrCreateOptionID("loop.kcomp", "The number of nearest neighbors of an object to be considered for computing its LOOP_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOOP_SCORE, must be an integer greater than 1.
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("loop.lambda", "The number of standard deviations to consider for density computation.");

  /**
   * Holds the value of {@link #KREACH_ID}.
   */
  int kreach;

  /**
   * Holds the value of {@link #KCOMP_ID}.
   */
  int kcomp;

  /**
   * Hold the value of {@link #LAMBDA_ID}.
   */
  double lambda;

  /**
   * Preprocessor Step 1
   */
  protected DistanceFunction<O, D> reachabilityDistanceFunction;

  /**
   * Preprocessor Step 2
   */
  protected DistanceFunction<O, D> comparisonDistanceFunction;

  /**
   * Include object itself in kNN neighborhood.
   */
  static boolean objectIsInKNN = false;

  /**
   * Constructor with parameters.
   * 
   * @param kreach
   * @param kcomp
   * @param reachabilityDistanceFunction
   * @param comparisonDistanceFunction
   * @param lambda
   */
  public LoOP(int kreach, int kcomp, DistanceFunction<O, D> reachabilityDistanceFunction, DistanceFunction<O, D> comparisonDistanceFunction, double lambda) {
    super();
    this.kreach = kreach;
    this.kcomp = kcomp;
    this.reachabilityDistanceFunction = reachabilityDistanceFunction;
    this.comparisonDistanceFunction = comparisonDistanceFunction;
    this.lambda = lambda;
  }

  /**
   * Get the kNN queries for the algorithm.
   * 
   * @param database Database
   * @param stepprog Progress logger
   * @return result
   */
  protected Pair<KNNQuery<O, D>, KNNQuery<O, D>> getKNNQueries(Database<O> database, StepProgress stepprog) {
    KNNQuery<O, D> knnComp;
    KNNQuery<O, D> knnReach;
    if(comparisonDistanceFunction.equals(reachabilityDistanceFunction)) {
      // We need each neighborhood twice - use "HEAVY" flag. 
      knnComp = database.getKNNQuery(comparisonDistanceFunction, Math.max(kreach, kcomp), DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
      // No optimized kNN query - use a preprocessor!
      if(knnComp == null) {
        if(stepprog != null) {
          stepprog.beginStep(1, "Materializing neighborhoods with respect to reference neighborhood distance function.", logger);
        }
        ListParameterization config = new ListParameterization();
        config.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, comparisonDistanceFunction);
        config.addParameter(MaterializeKNNPreprocessor.K_ID, kcomp);
        MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<O, D>(config);
        MaterializeKNNPreprocessor.Instance<O, D> instance = preproc.instantiate(database);
        knnComp = instance.getKNNQuery(database, comparisonDistanceFunction, kreach, DatabaseQuery.HINT_HEAVY_USE);
      }
      else {
        if(stepprog != null) {
          stepprog.beginStep(1, "Optimized neighborhoods provided by database.", logger);
        }
      }
      knnReach = knnComp;
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(1, "Not materializing distance functions, since we request each DBID once only.", logger);
      }
      knnComp = database.getKNNQuery(comparisonDistanceFunction, kreach);
      knnReach = database.getKNNQuery(reachabilityDistanceFunction, kcomp);
    }
    return new Pair<KNNQuery<O, D>, KNNQuery<O, D>>(knnComp, knnReach);
  }
  
  /**
   * Performs the LoOP algorithm on the given database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    final double sqrt2 = Math.sqrt(2.0);

    StepProgress stepprog = logger.isVerbose() ? new StepProgress(5) : null;

    Pair<KNNQuery<O, D>, KNNQuery<O, D>> pair = getKNNQueries(database, stepprog);
    KNNQuery<O, D> knnComp = pair.getFirst();
    KNNQuery<O, D> knnReach = pair.getSecond();

    // Assert we got something
    if(knnComp == null) {
      throw new AbortException("No kNN queries supported by database for comparison distance function.");
    }
    if(knnReach == null) {
      throw new AbortException("No kNN queries supported by database for density estimation distance function.");
    }

    // Probabilistic distances
    WritableDataStore<Double> pdists = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    {// computing PRDs
      if(stepprog != null) {
        stepprog.beginStep(3, "Computing pdists", logger);
      }
      FiniteProgress prdsProgress = logger.isVerbose() ? new FiniteProgress("pdists", database.size(), logger) : null;
      for(DBID id : database) {
        List<DistanceResultPair<D>> neighbors = knnReach.getKNNForDBID(id, kreach);
        double sqsum = 0.0;
        // use first kref neighbors as reference set
        int ks = 0;
        for(DistanceResultPair<D> neighbor : neighbors) {
          if(objectIsInKNN || neighbor.getID() != id) {
            double d = neighbor.getDistance().doubleValue();
            sqsum += d * d;
            ks++;
            if(ks >= kreach) {
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
        List<DistanceResultPair<D>> neighbors = knnComp.getKNNForDBID(id, kcomp);
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
    int kreach = getParameterKreach(config, kcomp);
    DistanceFunction<O, D> comparisonDistanceFunction = getParameterComparisonDistanceFunction(config);
    DistanceFunction<O, D> reachabilityDistanceFunction = getParameterReachabilityDistanceFunction(config);
    if (reachabilityDistanceFunction == null) {
      reachabilityDistanceFunction = comparisonDistanceFunction;
    }
    double lambda = getParameterLambda(config);
    if(config.hasErrors()) {
      return null;
    }
    return new LoOP<O, D>(kreach, kcomp, reachabilityDistanceFunction, comparisonDistanceFunction, lambda);
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
  protected static int getParameterKreach(Parameterization config, int kcomp) {
    final IntParameter param = new IntParameter(KREACH_ID, new GreaterConstraint(1), true);
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
  protected static <F extends DistanceFunction<?, ?>> F getParameterReachabilityDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
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