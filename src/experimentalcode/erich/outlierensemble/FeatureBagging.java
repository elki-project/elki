package experimentalcode.erich.outlierensemble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * A simple ensemble method called "Feature bagging" for outlier detection.
 * </p>
 * 
 * <p>
 * Since the proposed method is only sensible to run on multiple instances of
 * the same algorithm (due to incompatible score ranges), we do not allow using
 * arbitrary algorithms.
 * </p>
 * 
 * <p>
 * Reference: <br>
 * A. Lazarevic, V. Kumar: Feature Bagging for Outlier Detection<br />
 * In: Proc. of the 11th ACM SIGKDD international conference on Knowledge
 * discovery in data mining
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O>
 * @param <R>
 */
@Title("Feature Bagging for Outlier Detection")
@Reference(title = "Feature Bagging for Outlier Detection", authors = "A. Lazarevic, V. Kumar", booktitle = "Proc. of the 11th ACM SIGKDD international conference on Knowledge discovery in data mining", url = "http://dx.doi.org/10.1145/1081870.1081891")
public class FeatureBagging<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * OptionID for {@link #NUM_PARAM}
   */
  public static final OptionID NUM_ID = OptionID.getOrCreateOptionID("fbagging.num", "The number of instances to use in the ensemble.");

  /**
   * Parameter to specify the number of instances to use in the ensemble.
   * <p>
   * Key: {@code -fbagging.num}
   * </p>
   */
  private final IntParameter NUM_PARAM = new IntParameter(NUM_ID, new GreaterEqualConstraint(1));

  /**
   * OptionID for {@link #BREADTH_FLAG}
   */
  public static final OptionID BREADTH_ID = OptionID.getOrCreateOptionID("fbagging.breadth", "Use the breadth first combinations instead of the cumulative sum approach");

  /**
   * Feature bagging result ID
   */
  public static final AssociationID<Double> FEATUREBAGGING_ID = AssociationID.getOrCreateAssociationID("fbagging", Double.class);

  /**
   * The flag for {@link #BREADTH_ID}.
   * <p>
   * Key: {@code -fbagging.sum}
   * </p>
   */
  private final Flag BREADTH_FLAG = new Flag(BREADTH_ID);

  /**
   * Number of instances to use
   */
  protected int num = 1;

  /**
   * Cumulative sum or breadth first combinations
   */
  protected boolean breadth = false;

  /**
   * Random number generator for subspace choice
   */
  private Random RANDOM = new Random(0);

  /**
   * The parameters that were used to configure LOF.
   */
  private Collection<Pair<OptionID, Object>> lofparams;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public FeatureBagging(Parameterization config) {
    super(config);
    if(config.grab(NUM_PARAM)) {
      num = NUM_PARAM.getValue();
    }
    if(config.grab(BREADTH_FLAG)) {
      breadth = BREADTH_FLAG.getValue();
    }
    // setup LOF algorithm
    {
      TrackParameters track = new TrackParameters(config);
      ListParameterization predef = new ListParameterization();
      EuclideanDistanceFunction<O> d = new EuclideanDistanceFunction<O>();
      predef.addParameter(LOF.DISTANCE_FUNCTION_ID, d);
      predef.addParameter(LOF.REACHABILITY_DISTANCE_FUNCTION_ID, d);
      predef.addParameter(LOF.KNNQUERY_ID, PreprocessorKNNQuery.class);
      predef.addParameter(OptionID.ALGORITHM_VERBOSE, isVerbose());
      predef.addParameter(OptionID.ALGORITHM_TIME, isTime());
      ChainedParameterization chain = new ChainedParameterization(predef, track);
      chain.errorsTo(config);
      new LOF<O, D>(chain);
      predef.reportInternalParameterizationErrors(config);
      lofparams = track.getGivenParameters();
    }
  }

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    int dbdim = database.dimensionality();
    int mindim = dbdim / 2;
    int maxdim = dbdim - 1;

    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("LOF iterations", num, logger) : null;
    ArrayList<OutlierResult> results = new ArrayList<OutlierResult>(num);
    for(int i = 0; i < num; i++) {
      // Fill with all dimensions
      List<Integer> dims = new java.util.Vector<Integer>(dbdim);
      for(int d = 0; d < dbdim; d++) {
        dims.add(d + 1);
      }
      // Target dimensionality:
      int subdim = mindim + RANDOM.nextInt(maxdim - mindim);
      // Shrink the subspace to the destination size
      while(dims.size() > subdim) {
        dims.remove(RANDOM.nextInt(dims.size()));
      }
      // Configure distance function
      ListParameterization config = new ListParameterization();
      config.addParameter(DimensionsSelectingEuclideanDistanceFunction.DIMS_ID, dims);
      config.addParameter(LOF.DISTANCE_FUNCTION_ID, DimensionsSelectingEuclideanDistanceFunction.class);
      for(Pair<OptionID, Object> opt : lofparams) {
        config.addParameter(opt.first, opt.second);
      }
      //logger.verbose(config.toString());
      LOF<O, D> lof = new LOF<O, D>(config);
      config.failOnErrors();

      // run LOF and collect the result
      OutlierResult result = lof.run(database);
      results.add(result);
      if(prog != null) {
        prog.incrementProcessed(logger);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(logger);
    }
    
    final OutlierResult result;
    if (breadth) {
      throw new RuntimeException("Breadth-first not yet implemented!");
    } else {
      // Cumulative sum.
      WritableDataStore<Double> sumscore = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
      MinMax<Double> minmax = new MinMax<Double>();
      FiniteProgress cprog = logger.isVerbose() ? new FiniteProgress("Combining results", database.size(), logger) : null;      
      for (DBID id : database) {
        double sum = 0.0;
        for (OutlierResult r : results) {
          sum += r.getScores().getValueFor(id);
        }
        sumscore.put(id, sum);
        minmax.put(sum);
        cprog.incrementProcessed(logger);
      }
      if(cprog != null) {
        cprog.ensureCompleted(logger);
      }
      OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
      AnnotationResult<Double> scores = new AnnotationFromDataStore<Double>(FEATUREBAGGING_ID, sumscore);
      OrderingResult ordering = new OrderingFromDataStore<Double>(sumscore, true);
      result = new OutlierResult(meta , scores , ordering );
    }
    return result;
  }
}
