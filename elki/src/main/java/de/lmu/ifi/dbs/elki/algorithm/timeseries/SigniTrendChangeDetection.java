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
import de.lmu.ifi.dbs.elki.data.LabelList;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
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

  private double bias, halflife, minsigma;

  private boolean slowalpha;

  /**
   * Constructor
   *
   * @param bias beta term
   * @param halflife half-life for learning rate alpha
   * @param minsigma threshold for detecting a trend
   * @param slowalpha use corrected alpha
   */
  public SigniTrendChangeDetection(double bias, double halflife, double minsigma, boolean slowalpha) {
    this.bias = bias;
    this.halflife = halflife;
    this.minsigma = minsigma;
    this.slowalpha = slowalpha;
  }

  /**
   * Executes Signi-Trend for given relation
   *
   * @param relation relation to process
   * @param labellist labels of distinct timer series
   * @return list with all the detected trends for every time series
   */
  public ChangePointDetectionResult run(Relation<DoubleVector> relation, Relation<LabelList> labellist) {
    List<ChangePoints> result = new ArrayList<>();

    if(slowalpha) {
      for(DBIDIter realtion_iter = relation.getDBIDs().iter(); realtion_iter.valid(); realtion_iter.advance()) {
        result.add(new ChangePoints(detectTrendSlow(relation.get(realtion_iter).toArray())));
      }
    }
    else {
      for(DBIDIter realtion_iter = relation.getDBIDs().iter(); realtion_iter.valid(); realtion_iter.advance()) {
        result.add(new ChangePoints(detectTrend(relation.get(realtion_iter).toArray())));
      }
    }
    return new ChangePointDetectionResult("Signi-Trend Changepoints", "signitrend-changepoints", result, labellist);
  }

  /**
   * Performs the trend detection for a given time series Detects increases and
   * decreases in trend
   *
   * @param values time series
   * @return list of trend for given time series
   */
  private List<ChangePoint> detectTrend(double[] values) {
    List<ChangePoint> result = new ArrayList<>();

    double alpha = 1 - FastMath.exp(FastMath.log(0.5) / halflife);

    double ewma = values[0];
    double ewmavar = ewma * ewma;
    double[] sigma = new double[values.length];
    sigma[0] = 0;

    for(int i = 1; i < values.length; i++) {
      sigma[i] = (values[i] - ewma) / (FastMath.sqrt(ewmavar) + bias);
      double delta = values[i] - ewma;
      ewma += alpha * delta;
      ewmavar = (1 - alpha) * (ewmavar + alpha * delta * delta);
    }

    for(int i = 0; i < sigma.length; i++) {
      if(sigma[i] > minsigma || sigma[i] < -minsigma) {
        result.add(new ChangePoint(i, sigma[i]));
      }
    }

    return result;
  }

  /**
   * Performs the trend detection for a given time series Detects increases and
   * decreases in trend Learning rate alpha is adjusted with an updated weight
   *
   * @param values time series
   * @return list of trend for given time series
   */
  private List<ChangePoint> detectTrendSlow(double[] values) {
    List<ChangePoint> result = new ArrayList<>();

    double alpha = 1 - FastMath.exp(FastMath.log(0.5) / halflife);

    double ewma = values[0];
    double ewmavar = ewma * ewma;
    double[] sigma = new double[values.length];
    sigma[0] = 0;
    double weight = alpha;

    for(int i = 1; i < values.length; i++) {
      double inc = (1 - weight) * alpha;
      double alpha_cor = alpha / (weight * (1 - alpha) + alpha);
      weight = weight + inc;

      sigma[i] = (values[i] - ewma) / (FastMath.sqrt(ewmavar) + bias);
      double delta = values[i] - ewma;
      ewma += alpha_cor * delta;
      ewmavar = (1 - alpha) * (ewmavar + alpha_cor * delta * delta);
    }

    for(int i = 0; i < sigma.length; i++) {
      if(sigma[i] > minsigma || sigma[i] < -minsigma) {
        result.add(new ChangePoint(i, sigma[i]));
      }
    }

    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH, TypeUtil.LABELLIST);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static class Parameterizer extends AbstractParameterizer {

    public static final OptionID BIAS_ID = new OptionID("signitrend.bias", //
        "Bias");

    public static final OptionID HALFLIFE_ID = new OptionID("signitrend.halflife", //
        "Half time");

    public static final OptionID MINSIGMA_ID = new OptionID("signitrend.minsigma", //
        "Minimal Sigma");

    public static final OptionID SLOWLEARNING_ID = new OptionID("signitrend.slowlearning", //
        "Slow learning rate alpha");

    private double bias, halflife, minsigma;

    private boolean slowalpha;

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
      Flag slowalpha_parameter = new Flag(SLOWLEARNING_ID);
      if(config.grab(slowalpha_parameter)) {
        slowalpha = slowalpha_parameter.isTrue();
      }
    }

    @Override
    protected SigniTrendChangeDetection makeInstance() {
      return new SigniTrendChangeDetection(bias, halflife, minsigma, slowalpha);
    }
  }
}
