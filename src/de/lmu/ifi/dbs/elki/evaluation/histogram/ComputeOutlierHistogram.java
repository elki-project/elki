package de.lmu.ifi.dbs.elki.evaluation.histogram;

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
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 * 
 * The parameter {@code -hist.positive} specifies the class label of "positive"
 * hits.
 * 
 * @author Lisa Reichert
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
   * The object pattern to identify positive classes
   * <p>
   * Key: {@code -comphist.positive}
   * </p>
   */
  // TODO: ERICH: Make this a PatternParameter
  private final StringParameter POSITIVE_CLASS_NAME_PARAM = new StringParameter(POSITIVE_CLASS_NAME_ID);

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
  private final ObjectParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  private final ObjectParameter<ScalingFunction> SCALING_PARAM = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);

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
  private ScalingFunction scaling;

  /**
   * Flag to make split frequencies
   */
  private boolean splitfreq = false;

  public ComputeOutlierHistogram(Parameterization config) {
    super(config);
    if(config.grab(this, POSITIVE_CLASS_NAME_PARAM)) {
      positive_class_name = POSITIVE_CLASS_NAME_PARAM.getValue();
    }
    if(config.grab(this, ALGORITHM_PARAM)) {
      algorithm = ALGORITHM_PARAM.instantiateClass(config);
    }
    if(config.grab(this, BINS_PARAM)) {
      bins = BINS_PARAM.getValue();
    }
    if(config.grab(this, SCALING_PARAM)) {
      scaling = SCALING_PARAM.instantiateClass(config);
    }
    if(config.grab(this, SPLITFREQ_PARAM)) {
      splitfreq = SPLITFREQ_PARAM.getValue();
    }
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
    // first value for outliers, second for each object
    final AggregatingHistogram<Pair<Double, Double>, Pair<Double, Double>> hist;
    // If we have useful (finite) min/max, use these for binning.
    double min = scaling.getMin();
    double max = scaling.getMax();
    if(Double.isInfinite(min) || Double.isNaN(min) || Double.isInfinite(max) || Double.isNaN(max)) {
      hist = FlexiHistogram.DoubleSumDoubleSumHistogram(bins);
    }
    else {
      hist = AggregatingHistogram.DoubleSumDoubleSumHistogram(bins, min, max);
    }
    // first fill histogram only with values of outliers
    Pair<Double, Double> positive, negative;
    if(!splitfreq) {
      positive = new Pair<Double, Double>(0., 1. / ids.size());
      negative = new Pair<Double, Double>(1. / ids.size(), 0.);
    }
    else {
      positive = new Pair<Double, Double>(0., 1. / outlierIds.size());
      negative = new Pair<Double, Double>(1. / (ids.size() - outlierIds.size()), 0.);
    }
    ids.removeAll(outlierIds);
    // fill histogram with values of each object
    for(Integer id : ids) {
      double result = or.getScores().getValueFor(id);
      result = scaling.getScaled(result);
      hist.aggregate(result, negative);
    }
    for(Integer id : outlierIds) {
      double result = or.getScores().getValueFor(id);
      result = scaling.getScaled(result);
      hist.aggregate(result, positive);
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
   * Find an OutlierResult to work with.
   * 
   * @param database Database context
   * @param result Result object
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Database<O> database, Result result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors.size() > 0) {
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
}
