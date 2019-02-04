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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1],
 * by assuming a Gamma distribution on the data and evaluating the Gamma CDF.
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
public class MinusLogGammaScaling extends OutlierGammaScaling {
  /**
   * Maximum value seen
   */
  double max = 0;

  /**
   * Minimum value (after log step, so maximum again)
   */
  double mlogmax;

  /**
   * Constructor.
   */
  public MinusLogGammaScaling() {
    super(false);
  }

  @Override
  protected double preScale(double score) {
    assert (max > 0) : "prepare() was not run prior to using the scaling function.";
    return -FastMath.log(score / max) / mlogmax;
  }

  @Override
  public void prepare(OutlierResult or) {
    meta = or.getOutlierMeta();
    // Determine Minimum and Maximum.
    DoubleMinMax mm = new DoubleMinMax();
    DoubleRelation scores = or.getScores();
    for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
      double score = scores.doubleValue(id);
      if(!Double.isNaN(score) && !Double.isInfinite(score)) {
        mm.put(score);
      }
    }
    max = mm.getMax();
    mlogmax = -FastMath.log(mm.getMin() / max);
    // with the prescaling, do Gamma Scaling.
    MeanVariance mv = new MeanVariance();
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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MinusLogGammaScaling makeInstance() {
      return new MinusLogGammaScaling();
    }
  }
}
