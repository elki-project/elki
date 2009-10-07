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
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.FlexiHistogram;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 * 
 * The parameter {@code -hist.positive} specifies the class label of "positive"
 * hits.
 * 
 * @author Lisa
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */

public class ComputeOutlierHistogram<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * OptionID for {@link #POSITIVE_CLASS_NAME_PARAM}
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("comphist.positive", "Class label for the 'positive' class.");

  /**
   * OptionID for {@link #BINS_ID}
   */
  public static final OptionID BINS_ID = OptionID.getOrCreateOptionID("comphist.bins", "number of bins");

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * OptionID for {@link #SPLITFREQ_PARAM}
   */
  public static final OptionID SPLITFREQ_ID = OptionID.getOrCreateOptionID("histogram.splitfreq", "Use separate frequencies for outliers and non-outliers.");

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
   * number of bins for the histogram
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -comphist.bins}
   * </p>
   */
  private final IntParameter BINS_PARAM = new IntParameter(BINS_ID);

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
  private final ClassParameter<OutlierScalingFunction> SCALING_PARAM = new ClassParameter<OutlierScalingFunction>(SCALING_ID, OutlierScalingFunction.class, IdentityScaling.class.getName());

  /**
   * Flag to count frequencies of outliers and non-outliers separately
   * <p>
   * Key: {@code -histogram.splitfreq}
   * </p>
   */
  private final Flag SPLITFREQ_PARAM = new Flag(SPLITFREQ_ID);

  /**
   * Stores the "positive" class.
   */
  private String positive_class_name;

  /**
   * Number of bins
   */
  private int bins;

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
  private OutlierScalingFunction scaling;

  /**
   * Flag to make split frequencies
   */
  private boolean splitfreq = false;

  public ComputeOutlierHistogram() {
    super();
    addOption(POSITIVE_CLASS_NAME_PARAM);
    addOption(ALGORITHM_PARAM);
    addOption(BINS_PARAM);
    addOption(SCALING_PARAM);
    addOption(SPLITFREQ_PARAM);
  }

  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    AnnotationResult<Double> ann = getAnnotationResult(database, innerresult);
    scaling.prepare(database, innerresult, ann);

    Collection<Integer> ids = database.getIDs();
    Cluster<Model> positivecluster = getReferenceCluster(database, positive_class_name);
    Collection<Integer> outlierIds = positivecluster.getIDs();
    // first value for outliers, second for each object
    AggregatingHistogram<Pair<Double, Double>, Pair<Double, Double>> hist = FlexiHistogram.DoubleSumDoubleSumHistogram(bins);
    // first fill histogram only with values of outliers
    Pair<Double, Double> positive, negative;
    if (!splitfreq) {
      positive = new Pair<Double, Double>(0., 1. / ids.size());
      negative = new Pair<Double, Double>(1. / ids.size(), 0.);
    } else {
      positive = new Pair<Double, Double>(0., 1. / outlierIds.size());
      negative = new Pair<Double, Double>(1. / (ids.size() - outlierIds.size()), 0.);      
    }
    for(Integer id : outlierIds) {
      double result = ann.getValueFor(id);
      result = scaling.getScaled(result);
      hist.aggregate(result, positive);
    }
    ids.removeAll(outlierIds);
    // fill histogram with values of each object
    for(Integer id : ids) {
      double result = ann.getValueFor(id);
      result = scaling.getScaled(result);
      hist.aggregate(result, negative);
    }

    // turn into Collection

    Collection<DoubleVector> collHist = new ArrayList<DoubleVector>(hist.getNumBins());
    for(Pair<Double, Pair<Double, Double>> ppair : hist) {
      Pair<Double, Double> data = ppair.getSecond();
      DoubleVector row = new DoubleVector(new double[] { ppair.getFirst(), data.getFirst(), data.getSecond() });
      collHist.add(row);
    }

    result = new MultiResult();
    result.addResult(innerresult);
    result.addResult(new HistogramResult<DoubleVector>(collHist));

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
   * Find an AnnotationResult that contains Doubles.
   * 
   * @param database Database context
   * @param result Result object
   * @return Iterator to work with
   */
  @SuppressWarnings("unchecked")
  private AnnotationResult<Double> getAnnotationResult(Database<O> database, Result result) {
    List<AnnotationResult<?>> annotations = ResultUtil.getAnnotationResults(result);
    for(AnnotationResult<?> ann : annotations) {
      if(Double.class.isAssignableFrom(ann.getAssociationID().getType())) {
        return (AnnotationResult<Double>) ann;
      }
    }
    throw new IllegalStateException("Comparison algorithm expected at least one Annotation<Double> result, got " + annotations.size() + " annotation results.");
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
    bins = BINS_PARAM.getValue();
    // algorithm
    algorithm = ALGORITHM_PARAM.instantiateClass();
    addParameterizable(algorithm);
    remainingParameters = algorithm.setParameters(remainingParameters);
    // scaling function
    scaling = SCALING_PARAM.instantiateClass();
    if (scaling instanceof Parameterizable) {
      Parameterizable param = (Parameterizable) scaling;
      addParameterizable(param);
      remainingParameters = param.setParameters(remainingParameters);
    }
    if (SPLITFREQ_PARAM.isSet()) {
      splitfreq = true;
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
