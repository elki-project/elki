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
package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1]
 * by assuming a Gamma distribution on the values.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Interpreting and Unifying Outlier Scores<br>
 * Proc. 11th SIAM International Conference on Data Mining (SDM 2011)
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Interpreting and Unifying Outlier Scores", //
    booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM 2011)", //
    url = "https://doi.org/10.1137/1.9781611972818.2", //
    bibkey = "DBLP:conf/sdm/KriegelKSZ11")
public class OutlierGammaScaling implements OutlierScaling {
  /**
   * Gamma parameter k
   */
  double k;

  /**
   * Gamma parameter theta
   */
  double theta;

  /**
   * Score at the mean, for cut-off.
   */
  double atmean = 0.5;

  /**
   * Store flag to Normalize data before curve fitting.
   */
  boolean normalize = false;

  /**
   * Keep a reference to the outlier score meta, for normalization.
   */
  OutlierScoreMeta meta = null;

  /**
   * Constructor.
   * 
   * @param normalize Normalization flag
   */
  public OutlierGammaScaling(boolean normalize) {
    super();
    this.normalize = normalize;
  }

  @Override
  public double getScaled(double value) {
    assert (theta > 0) : "prepare() was not run prior to using the scaling function.";
    value = preScale(value);
    return Double.isNaN(value) || Double.isInfinite(value) ? 1. : //
        Math.max(0, (GammaDistribution.regularizedGammaP(k, value / theta) - atmean) / (1 - atmean));
  }

  @Override
  public void prepare(OutlierResult or) {
    meta = or.getOutlierMeta();
    MeanVariance mv = new MeanVariance();
    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double score = preScale(scores.doubleValue(id));
      if(!Double.isNaN(score) && !Double.isInfinite(score)) {
        mv.put(score);
      }
    }
    final double mean = mv.getMean(), var = mv.getSampleVariance();
    k = (mean * mean) / var;
    theta = var / mean;
    atmean = GammaDistribution.regularizedGammaP(k, mean / theta);
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    MeanVariance mv = new MeanVariance();
    final int size = adapter.size(array);
    for(int i = 0; i < size; i++) {
      double score = preScale(adapter.getDouble(array, i));
      if(!Double.isNaN(score) && !Double.isInfinite(score)) {
        mv.put(score);
      }
    }
    final double mean = mv.getMean(), var = mv.getSampleVariance();
    k = (mean * mean) / var;
    theta = var / mean;
    atmean = GammaDistribution.regularizedGammaP(k, mean / theta);
  }

  /**
   * Normalize data if necessary.
   * <p>
   * Note: this is overridden by {@link MinusLogGammaScaling}!
   * 
   * @param score Original score
   * @return Normalized score.
   */
  protected double preScale(double score) {
    return normalize ? meta.normalizeScore(score) : score;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Normalization flag.
     */
    public static final OptionID NORMALIZE_ID = new OptionID("gammascale.normalize", "Regularize scores before using Gamma scaling.");

    /**
     * Store flag to Normalize data before curve fitting.
     */
    protected boolean normalize = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag normalizeF = new Flag(NORMALIZE_ID);
      if(config.grab(normalizeF)) {
        normalize = normalizeF.getValue();
      }
    }

    @Override
    protected OutlierGammaScaling makeInstance() {
      return new OutlierGammaScaling(normalize);
    }
  }
}
