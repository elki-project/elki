/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.evaluation.Evaluator;
import elki.math.linearalgebra.VMath;
import elki.result.HistogramResult;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.utilities.datastructures.histogram.AbstractObjDynamicHistogram;
import elki.utilities.datastructures.histogram.ObjHistogram;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.PatternParameter;
import elki.utilities.scaling.IdentityScaling;
import elki.utilities.scaling.ScalingFunction;
import elki.utilities.scaling.outlier.OutlierScaling;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 * <p>
 * The parameter {@code -hist.positive} specifies the class label of "positive"
 * hits.
 *
 * @author Lisa Reichert
 * @author Erich Schubert
 * @since 0.3
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - - - OutlierResult
 * @has - - - ScalingFunction
 * @navhas - create - HistogramResult
 */
public class ComputeOutlierHistogram implements Evaluator {
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
  public HistogramResult evaluateOutlierResult(Database database, OutlierResult or) {
    if(scaling instanceof OutlierScaling) {
      ((OutlierScaling) scaling).prepare(or);
    }

    ModifiableDBIDs ids = DBIDUtil.newHashSet(or.getScores().getDBIDs());
    DBIDs outlierIds = DatabaseUtil.getObjectsByLabelMatch(database, positiveClassName);
    // first value for outliers, second for each object
    // If we have useful (finite) min/max, use these for binning.
    double min = scaling.getMin(), max = scaling.getMax();
    final ObjHistogram<double[]> hist;
    if(Double.isInfinite(min) || Double.isNaN(min) || Double.isInfinite(max) || Double.isNaN(max)) {
      hist = new AbstractObjDynamicHistogram<double[]>(bins) {
        @Override
        public double[] aggregate(double[] first, double[] second) {
          return VMath.plusEquals(first, second);
        }

        @Override
        protected double[] makeObject() {
          return new double[2];
        }

        @Override
        protected double[] cloneForCache(double[] data) {
          return data.clone();
        }

        @Override
        protected double[] downsample(Object[] data, int start, int end, int size) {
          double[] sum = new double[2];
          for(int i = start; i < end; i++) {
            Object p = data[i];
            if(p != null) {
              VMath.plusEquals(sum, (double[]) p);
            }
          }
          return sum;
        }
      };
    }
    else {
      hist = new ObjHistogram<double[]>(bins, min, max, () -> {
        return new double[2];
      });
    }

    // first fill histogram only with values of outliers
    double negative = 1. / ids.size(), positive = negative;
    if(splitfreq) {
      negative = 1. / (ids.size() - outlierIds.size());
      positive = 1. / outlierIds.size();
    }
    ids.removeDBIDs(outlierIds);
    // fill histogram with values of each object
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double result = or.getScores().doubleValue(iter);
      result = scaling.getScaled(result);
      if(result > Double.NEGATIVE_INFINITY && result < Double.POSITIVE_INFINITY) {
        hist.get(result)[0] += negative;
      }
    }
    for(DBIDIter iter = outlierIds.iter(); iter.valid(); iter.advance()) {
      double result = or.getScores().doubleValue(iter);
      result = scaling.getScaled(result);
      if(result > Double.NEGATIVE_INFINITY && result < Double.POSITIVE_INFINITY) {
        hist.get(result)[1] += positive;
      }
    }
    Collection<double[]> collHist = new ArrayList<>(hist.getNumBins());
    for(ObjHistogram<double[]>.Iter iter = hist.iter(); iter.valid(); iter.advance()) {
      double[] data = iter.getValue();
      collHist.add(new double[] { iter.getCenter(), data[0], data[1] });
    }
    HistogramResult result = new HistogramResult(collHist);
    Metadata.of(result).setLongName("Outlier Score Histogram");
    return result;
  }

  @Override
  public void processNewResult(Object result) {
    final Database db = ResultUtil.findDatabase(result);
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors == null || ors.isEmpty()) {
      // LOG.warning("No outlier results found for
      // "+ComputeOutlierHistogram.class.getSimpleName());
      return;
    }

    for(OutlierResult or : ors) {
      Metadata.hierarchyOf(or).addChild(evaluateOutlierResult(db, or));
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * The object pattern to identify positive classes
     */
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("comphist.positive", "Class label for the 'positive' class.");

    /**
     * number of bins for the histogram
     */
    public static final OptionID BINS_ID = new OptionID("comphist.bins", "number of bins");

    /**
     * Parameter to specify a scaling function to use.
     */
    public static final OptionID SCALING_ID = new OptionID("comphist.scaling", "Class to use as scaling function.");

    /**
     * Flag to count frequencies of outliers and non-outliers separately
     */
    public static final OptionID SPLITFREQ_ID = new OptionID("histogram.splitfreq", "Use separate frequencies for outliers and non-outliers.");

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
    public void configure(Parameterization config) {
      new PatternParameter(POSITIVE_CLASS_NAME_ID) //
          .setOptional(true) //
          .grab(config, x -> positiveClassName = x);
      new IntParameter(BINS_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> bins = x);
      new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class) //
          .grab(config, x -> scaling = x);
      new Flag(SPLITFREQ_ID).grab(config, x -> splitfreq = x);
    }

    @Override
    public ComputeOutlierHistogram make() {
      return new ComputeOutlierHistogram(positiveClassName, bins, scaling, splitfreq);
    }
  }
}
