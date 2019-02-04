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
package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * Signi-Trend detection algorithm applies to a single time-series.
 * <p>
 * This is not a complete implementation of the method, but a modified
 * (two-sided) version of the significance score use in Signi-Trend for change
 * detection. The hashing and scalability parts of Signi-Trend are not
 * applicable here.
 * <p>
 * This implementation currently does not use timestamps, and thus only works
 * for fixed-interval measurements. It could be extended to allow dynamic data
 * windows by adjusting the alpha parameter based on time deltas.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Michael Weiler, Hans-Peter Kriegel<br>
 * Signi-Trend: scalable detection of emerging topics in textual streams by
 * hashed significance thresholds<br>
 * Proc. 20th ACM SIGKDD international conference on Knowledge discovery and
 * data mining
 * <p>
 * TODO: add support for dynamic time, and optimize for sparse vectors.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @composed - - - Instance
 * @assoc - - - ChangePoints
 */
@Title("Signi-Trend: scalable detection of emerging topics in textual streams by hashed significance thresholds")
@Reference(authors = "Erich Schubert, Michael Weiler, Hans-Peter Kriegel", //
    title = "Signi-Trend: scalable detection of emerging topics in textual streams by hashed significance thresholds", //
    booktitle = "Proc. 20th ACM SIGKDD international conference on Knowledge discovery and data mining", //
    url = "https://doi.org/10.1145/2623330.2623740", //
    bibkey = "DBLP:conf/kdd/SchubertWK14")
@Priority(Priority.RECOMMENDED)
public class SigniTrendChangeDetection extends AbstractAlgorithm<ChangePoints> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SigniTrendChangeDetection.class);

  /**
   * Exponential aging parameter.
   */
  private double alpha;

  /**
   * Bias for small values.
   */
  private double bias;

  /**
   * Minimum sigma to report.
   */
  private double minsigma;

  /**
   * Constructor
   *
   * @param halflife half-life for learning rate alpha
   * @param bias beta term
   * @param minsigma threshold for detecting a trend
   */
  public SigniTrendChangeDetection(double halflife, double bias, double minsigma) {
    // Compute alpha from half-life parameter:
    this.alpha = 1. - FastMath.exp(FastMath.log(0.5) / halflife);
    this.bias = bias;
    this.minsigma = minsigma;
  }

  /**
   * Executes Signi-Trend for given relation
   *
   * @param relation relation to process
   * @return list with all the detected trends for every time series
   */
  public ChangePoints run(Relation<NumberVector> relation) {
    return new Instance().run(relation);
  }

  /**
   * Instance for one data set.
   * 
   * @author Erich Schubert
   */
  protected class Instance {
    /**
     * Moving average and variance.
     */
    protected double[] ewma, ewmv;

    /**
     * Current weight:
     */
    protected double weight;

    /**
     * Constructor.
     */
    public Instance() {
      super();
    }

    /**
     * Process a relation.
     * 
     * @param relation Data relation
     * @return Change points
     */
    public ChangePoints run(Relation<NumberVector> relation) {
      if(!(relation.getDBIDs() instanceof ArrayDBIDs)) {
        throw new AbortException("This implementation may only be used on static databases, with ArrayDBIDs to provide a clear order.");
      }
      final ArrayDBIDs ids = (ArrayDBIDs) relation.getDBIDs();
      final int dim = RelationUtil.dimensionality(relation);
      ewma = new double[dim];
      ewmv = new double[dim];
      weight = 0.;

      ChangePoints changepoints = new ChangePoints("Signi-Trend Changepoints", "signitrend-changepoints");
      WritableDoubleDataStore vals = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED | DataStoreFactory.HINT_STATIC);
      DoubleMinMax mm = new DoubleMinMax();
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        double absmax = processRow(iter, relation.get(iter), changepoints);
        vals.putDouble(iter, absmax); // Store absolute maximum
        mm.put(absmax);
      }
      OutlierScoreMeta meta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0, Double.POSITIVE_INFINITY, 0.);
      DoubleRelation scores = new MaterializedDoubleRelation("Signi-Trend scores", "signitrend-scores", vals, relation.getDBIDs());
      changepoints.addChildResult(new OutlierResult(meta, scores));
      return changepoints;
    }

    /**
     * Process one row, assuming a constant time interval.
     * 
     * @param iter Row identifier for reporting.
     * @param row Data row
     * @param changepoints Change points result for output
     * @return absolute maximum deviation.
     */
    private double processRow(DBIDRef iter, NumberVector row, ChangePoints changepoints) {
      if(!(weight > 0.)) {
        // Cold start.
        for(int d = 0; d < row.getDimensionality(); d++) {
          double v = ewma[d] = row.doubleValue(d);
          ewmv[d] = v * v;
        }
        weight = alpha;
        return 0.;
      }
      double alpha = SigniTrendChangeDetection.this.alpha;
      double minsigma = SigniTrendChangeDetection.this.minsigma;
      // Adjust alpha until the difference is neglibile
      if(weight < 1.) {
        final double inc = (1 - weight) * alpha; // Weight increment
        alpha = alpha / (weight * (1 - alpha) + alpha);
        weight += inc;
      }

      double absmax = 0.;
      for(int d = 0; d < row.getDimensionality(); d++) {
        final double v = row.doubleValue(d);
        // Old estimate:
        final double avg = ewma[d], var = ewmv[d];
        // Change detection (using previous estimate!)
        double sigma = (v - avg) / (FastMath.sqrt(var) + bias);
        if(sigma >= minsigma || sigma <= -minsigma) {
          changepoints.add(iter, d, sigma);
        }
        // Track maximum of all columns
        absmax = sigma > absmax ? sigma : -sigma > absmax ? -sigma : absmax;

        // Update estimates:
        final double deli = v - avg;
        final double inci = alpha * deli;
        ewma[d] += inci;
        ewmv[d] = (1 - alpha) * (var + inci * deli);
      }
      return absmax;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for half-life aging.
     */
    public static final OptionID HALFLIFE_ID = new OptionID("signitrend.halflife", //
        "Half-life time: number of time steps until data has lost half its weight.");

    /**
     * Bias adjustment for chance
     */
    public static final OptionID BIAS_ID = new OptionID("signitrend.bias", //
        "Adjustment for chance: a small constant corresponding to background noise levels.");

    /**
     * Sigma reporting threshold.
     */
    public static final OptionID MINSIGMA_ID = new OptionID("signitrend.minsigma", //
        "Significance threshold for reporting");

    /**
     * Half-life aging parameter.
     */
    private double halflife;

    /**
     * Bias for small values.
     */
    private double bias;

    /**
     * Minimum sigma to report.
     */
    private double minsigma;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter halflifeP = new DoubleParameter(HALFLIFE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(halflifeP)) {
        halflife = halflifeP.doubleValue();
      }
      DoubleParameter biasP = new DoubleParameter(BIAS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(biasP)) {
        bias = biasP.doubleValue();
      }
      DoubleParameter minsigmaP = new DoubleParameter(MINSIGMA_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(minsigmaP)) {
        minsigma = minsigmaP.getValue();
      }
    }

    @Override
    protected SigniTrendChangeDetection makeInstance() {
      return new SigniTrendChangeDetection(halflife, bias, minsigma);
    }
  }
}
