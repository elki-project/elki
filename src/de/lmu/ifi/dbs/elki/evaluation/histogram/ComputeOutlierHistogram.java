package de.lmu.ifi.dbs.elki.evaluation.histogram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.FlexiHistogram;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
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
 * @apiviz.landmark
 * @apiviz.has ScalingFunction
 * @apiviz.has HistogramResult oneway - - «create»
 */
public class ComputeOutlierHistogram implements Evaluator {
  /**
   * Logger for debugging.
   */
  protected static final Logging logger = Logging.getLogger(ComputeOutlierHistogram.class);

  /**
   * The object pattern to identify positive classes
   * <p>
   * Key: {@code -comphist.positive}
   * </p>
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("comphist.positive", "Class label for the 'positive' class.");

  /**
   * number of bins for the histogram
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -comphist.bins}
   * </p>
   */
  public static final OptionID BINS_ID = OptionID.getOrCreateOptionID("comphist.bins", "number of bins");

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * Flag to count frequencies of outliers and non-outliers separately
   * <p>
   * Key: {@code -histogram.splitfreq}
   * </p>
   */
  public static final OptionID SPLITFREQ_ID = OptionID.getOrCreateOptionID("histogram.splitfreq", "Use separate frequencies for outliers and non-outliers.");

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName = null;

  /**
   * Number of bins
   */
  private int bins;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Flag to make split frequencies
   */
  private boolean splitfreq = false;

  /**
   * Constructor.
   * 
   * @param positive_class_name Class name
   * @param bins Bins
   * @param scaling Scaling
   * @param splitfreq Scale inlier and outlier frequencies independently
   */
  public ComputeOutlierHistogram(Pattern positive_class_name, int bins, ScalingFunction scaling, boolean splitfreq) {
    super();
    this.positiveClassName = positive_class_name;
    this.bins = bins;
    this.scaling = scaling;
    this.splitfreq = splitfreq;
  }

  /**
   * Evaluate a single outlier result as histogram.
   * 
   * @param database Database to process
   * @param or Outlier result
   * @return Result
   */
  public HistogramResult<DoubleVector> evaluateOutlierResult(Database<?> database, OutlierResult or) {
    if(scaling instanceof OutlierScalingFunction) {
      OutlierScalingFunction oscaling = (OutlierScalingFunction) scaling;
      oscaling.prepare(database.getIDs(), or);
    }

    ModifiableDBIDs ids = DBIDUtil.newHashSet(database.getIDs());
    DBIDs outlierIds = DatabaseUtil.getObjectsByLabelMatch(database, positiveClassName);
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
    ids.removeDBIDs(outlierIds);
    // fill histogram with values of each object
    for(DBID id : ids) {
      double result = or.getScores().getValueFor(id);
      result = scaling.getScaled(result);
      hist.aggregate(result, negative);
    }
    for(DBID id : outlierIds) {
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
    return new HistogramResult<DoubleVector>("Outlier Score Histogram", "outlier-histogram", collHist);
  }

  @Override
  public void processResult(Database<?> db, Result result, ResultHierarchy hierarchy) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors == null || ors.size() <= 0) {
      // logger.warning("No outlier results found for "+ComputeOutlierHistogram.class.getSimpleName());
      return;
    }

    for(OutlierResult or : ors) {
      hierarchy.add(or, evaluateOutlierResult(db, or));
    }
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<?> normalization) {
    // Ignore normalizations.
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Stores the "positive" class.
     */
    protected Pattern positiveClassName = null;

    /**
     * Number of bins
     */
    protected int bins;

    /**
     * Scaling function to use
     */
    protected ScalingFunction scaling;

    /**
     * Flag to make split frequencies
     */
    protected boolean splitfreq = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID, true);
      if(config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }

      IntParameter binsP = new IntParameter(BINS_ID, new GreaterConstraint(1));
      if(config.grab(binsP)) {
        bins = binsP.getValue();
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }

      Flag splitfreqF = new Flag(SPLITFREQ_ID);
      if(config.grab(splitfreqF)) {
        splitfreq = splitfreqF.getValue();
      }

    }

    @Override
    protected ComputeOutlierHistogram makeInstance() {
      return new ComputeOutlierHistogram(positiveClassName, bins, scaling, splitfreq);
    }
  }
}