package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1]
 * by assuming a Gamma distribution on the values.
 * 
 * @author Erich Schubert
 */
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "Interpreting and Unifying Outlier Scores", booktitle = "Proc. 11th SIAM International Conference on Data Mining (SDM), Mesa, AZ, 2011", url = "http://www.dbs.ifi.lmu.de/~zimek/publications/SDM2011/SDM11-outlier-preprint.pdf")
public class OutlierGammaScaling implements OutlierScalingFunction {
  /**
   * Normalization flag.
   * 
   * <pre>
   * -gammascale.normalize
   * </pre>
   */
  public static final OptionID NORMALIZE_ID = OptionID.getOrCreateOptionID("gammascale.normalize", "Regularize scores before using Gamma scaling.");

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
    if(Double.isNaN(value) || Double.isInfinite(value)) {
      return 1.0;
    }
    return Math.max(0, (MathUtil.regularizedGammaP(k, value / theta) - atmean) / (1 - atmean));
  }

  @Override
  public void prepare(OutlierResult or) {
    meta = or.getOutlierMeta();
    MeanVariance mv = new MeanVariance();
    for(DBID id : or.getScores().iterDBIDs()) {
      double score = or.getScores().get(id);
      score = preScale(score);
      if(!Double.isNaN(score) && !Double.isInfinite(score)) {
        mv.put(score);
      }
    }
    final double mean = mv.getMean();
    final double var = mv.getSampleVariance();
    k = (mean * mean) / var;
    theta = var / mean;
    atmean = MathUtil.regularizedGammaP(k, mean / theta);
    // logger.warning("Mean:"+mean+" Var:"+var+" Theta: "+theta+" k: "+k+" valatmean"+atmean);
  }

  /**
   * Normalize data if necessary.
   * 
   * Note: this is overridden by {@link MinusLogGammaScaling}!
   * 
   * @param score Original score
   * @return Normalized score.
   */
  protected double preScale(double score) {
    if(normalize) {
      score = meta.normalizeScore(score);
    }
    return score;
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
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