package de.lmu.ifi.dbs.elki.algorithm.timeseries;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.ChangePointDetectionResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * Signi-Trend detection algorithm applies to a single time-series.
 *
 * This is not a complete implementation of the method, but a modified
 * (two-sided) version of the significance score use in Signi-Trend for change
 * detection. The hashing and scalability parts of Signi-Trend are not
 * applicable here.
 * 
 * Reference:
 * <p>
 * E. Schubert, M. Weiler, H. Kriegel<br />
 * Signi-Trend: scalable detection of emerging topics in textual streams by
 * hashed significance thresholds<br />
 * Proc. 20th ACM SIGKDD international conference on Knowledge discovery and
 * data mining
 * </p>
 *
 * @author Sebastian Rühl
 * @author Erich Schubert
 */
@Title("Signi-Trend: scalable detection of emerging topics in textual streams by hashed significance thresholds")
@Reference(authors = "E. Schubert, M. Weiler, H. Kriegel", //
    title = "Signi-Trend: scalable detection of emerging topics in textual streams by hashed significance thresholds", //
    booktitle = "Proc. 20th ACM SIGKDD international conference on Knowledge discovery and data mining", //
    url = "http://dx.doi.org/10.1145/2623330.2623740")
public class SigniTrendChangeDetection extends AbstractAlgorithm<ChangePointDetectionResult> {
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
  public ChangePointDetectionResult run(Relation<DoubleVector> relation) {
    List<ChangePoints> result = new ArrayList<>();

    for(DBIDIter realtion_iter = relation.getDBIDs().iter(); realtion_iter.valid(); realtion_iter.advance()) {
      result.add(new ChangePoints(detectTrend(relation.get(realtion_iter).toArray())));
    }
    return new ChangePointDetectionResult("Signi-Trend Changepoints", "signitrend-changepoints", result);
  }

  /**
   * Performs the trend detection for a given time series Detects increases and
   * decreases in trend Learning rate alpha is adjusted with an updated weight
   *
   * @param values time series
   * @return list of trend for given time series
   */
  private List<ChangePoint> detectTrend(double[] values) {
    List<ChangePoint> result = new ArrayList<>();

    // Cold start:
    double ewma = values[0];
    double ewmv = ewma * ewma;
    double weight = alpha;

    for(int i = 1; i < values.length; i++) {
      // Adjusted alpha will converge to alpha.
      double inc = (1 - weight) * alpha;
      double alph = alpha / (weight * (1 - alpha) + alpha);
      weight += inc;

      // Compare to previous estimate:
      double sigma = (values[i] - ewma) / (FastMath.sqrt(ewmv) + bias);
      if(sigma > minsigma || sigma < -minsigma) {
        result.add(new ChangePoint(i, sigma));
      }

      final double deli = values[i] - ewma;
      final double inci = alph * deli;
      ewma += inci;
      ewmv = (1 - alph) * (ewmv + inci * deli);
    }

    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static class Parameterizer extends AbstractParameterizer {

    public static final OptionID HALFLIFE_ID = new OptionID("signitrend.halflife", //
        "Half time");

    public static final OptionID BIAS_ID = new OptionID("signitrend.bias", //
        "Bias");

    public static final OptionID MINSIGMA_ID = new OptionID("signitrend.minsigma", //
        "Minimal Sigma");

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
      DoubleParameter bias_parameter = new DoubleParameter(BIAS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(bias_parameter)) {
        bias = bias_parameter.getValue();
      }
      DoubleParameter halflife_parameter = new DoubleParameter(HALFLIFE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(halflife_parameter)) {
        halflife = halflife_parameter.getValue();
      }
      DoubleParameter minsigma_parameter = new DoubleParameter(MINSIGMA_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(minsigma_parameter)) {
        minsigma = minsigma_parameter.getValue();
      }
    }

    @Override
    protected SigniTrendChangeDetection makeInstance() {
      return new SigniTrendChangeDetection(halflife, bias, minsigma);
    }
  }
}
