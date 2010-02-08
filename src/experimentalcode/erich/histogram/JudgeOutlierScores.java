package experimentalcode.erich.histogram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 * 
 * The parameter {@code -hist.positive} specifies the class label of "positive"
 * hits.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */

public class JudgeOutlierScores<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * OptionID for {@link #POSITIVE_CLASS_NAME_PARAM}
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("comphist.positive", "Class label for the 'positive' class.");

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -comphist.positive}
   * </p>
   */
  private final PatternParameter POSITIVE_CLASS_NAME_PARAM = new PatternParameter(POSITIVE_CLASS_NAME_ID);

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ClassParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ClassParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  private final ClassParameter<ScalingFunction> SCALING_PARAM = new ClassParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class.getName());

  /**
   * Stores the "positive" class.
   */
  private String positive_class_name;

  /**
   * Holds the algorithm to run.
   */
  private Algorithm<O, Result> algorithm;

  /**
   * Stores the result object.
   */
  private MultiResult result;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  public JudgeOutlierScores() {
    super();
    addOption(POSITIVE_CLASS_NAME_PARAM);
    addOption(ALGORITHM_PARAM);
    addOption(SCALING_PARAM);
  }

  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    OutlierResult or = getOutlierResult(database, innerresult);
    if(scaling instanceof OutlierScalingFunction) {
      OutlierScalingFunction oscaling = (OutlierScalingFunction) scaling;
      oscaling.prepare(database, innerresult, or);
    }

    Collection<Integer> ids = database.getIDs();
    Cluster<Model> positivecluster = getReferenceCluster(database, positive_class_name);
    Collection<Integer> outlierIds = positivecluster.getIDs();
    ids.removeAll(outlierIds);

    final ScalingFunction innerScaling;
    // If we have useful (finite) min/max, use these for binning.
    double min = scaling.getMin();
    double max = scaling.getMax();
    if(Double.isInfinite(min) || Double.isNaN(min) || Double.isInfinite(max) || Double.isNaN(max)) {
      innerScaling = new IdentityScaling();
      // TODO: does the outlier score give us this guarantee?
      logger.warning("JudgeOutlierScores expects values between 0.0 and 1.0, but we don't have such a guarantee by the scaling function: min:"+min+" max:"+max);
    }
    else {
      if (min == 0.0 && max == 1.0) {
        innerScaling = new IdentityScaling();
      } else {
        innerScaling = new LinearScaling(1.0 / (max - min), -min);
      }
    }

    double posscore = 0.0;
    double negscore = 0.0;
    // fill histogram with values of each object
    for(Integer id : ids) {
      double result = or.getScores().getValueFor(id);
      result = innerScaling.getScaled(scaling.getScaled(result));
      posscore += (1.0 - result);
    }
    for(Integer id : outlierIds) {
      double result = or.getScores().getValueFor(id);
      result = innerScaling.getScaled(scaling.getScaled(result));
      negscore += result;
    }
    posscore /= ids.size();
    negscore /= outlierIds.size();

    logger.verbose("Scores: " + posscore + " " + negscore);

    result = ResultUtil.ensureMultiResult(innerresult);
    
    ArrayList<DoubleVector> s = new ArrayList<DoubleVector>(1);
    s.add(new DoubleVector(new double[]{(posscore+negscore)/2,posscore, negscore}));
    result.addResult(new CollectionResult<DoubleVector>(s));

    return result;
  }

  /**
   * Find the "positive" reference cluster using a by label clustering.
   * 
   * @param database Database to search in
   * @param class_name Cluster name
   * @return found cluster or it throws an exception.
   */
  private Cluster<Model> getReferenceCluster(Database<O> database, String class_name) {
    ByLabelHierarchicalClustering<O> reference = new ByLabelHierarchicalClustering<O>();
    Clustering<Model> refc = reference.run(database);
    for(Cluster<Model> clus : refc.getAllClusters()) {
      if(clus.getNameAutomatic().compareToIgnoreCase(class_name) == 0) {
        return clus;
      }
    }
    throw new IllegalStateException("'Positive' cluster not found - cannot compute a Histogram value without a reference set.");
  }

  /**
   * Find an OutlierResult to work with.
   * 
   * @param database Database context
   * @param result Result object
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Database<O> database, Result result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if (ors.size() > 0) {
      return ors.get(0);
    }
    throw new IllegalStateException("Comparison algorithm expected at least one outlier result.");
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultiResult getResult() {
    return result;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    positive_class_name = POSITIVE_CLASS_NAME_PARAM.getValue();
    // algorithm
    algorithm = ALGORITHM_PARAM.instantiateClass();
    addParameterizable(algorithm);
    remainingParameters = algorithm.setParameters(remainingParameters);
    // scaling function
    scaling = SCALING_PARAM.instantiateClass();
    if(scaling instanceof Parameterizable) {
      Parameterizable param = (Parameterizable) scaling;
      addParameterizable(param);
      remainingParameters = param.setParameters(remainingParameters);
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
