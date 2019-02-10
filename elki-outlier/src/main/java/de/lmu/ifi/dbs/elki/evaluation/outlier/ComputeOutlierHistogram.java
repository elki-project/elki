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
package de.lmu.ifi.dbs.elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.AbstractObjDynamicHistogram;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.ObjHistogram;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 *
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
@Alias({ "de.lmu.ifi.dbs.elki.evaluation.histogram.ComputeOutlierHistogram" })
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
    return new HistogramResult("Outlier Score Histogram", "outlier-histogram", collHist);
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    final Database db = ResultUtil.findDatabase(hier);
    List<OutlierResult> ors = ResultUtil.filterResults(hier, newResult, OutlierResult.class);
    if(ors == null || ors.isEmpty()) {
      // LOG.warning("No outlier results found for
      // "+ComputeOutlierHistogram.class.getSimpleName());
      return;
    }

    for(OutlierResult or : ors) {
      db.getHierarchy().add(or, evaluateOutlierResult(db, or));
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID) //
          .setOptional(true);
      if(config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }

      IntParameter binsP = new IntParameter(BINS_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(binsP)) {
        bins = binsP.getValue();
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);
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
