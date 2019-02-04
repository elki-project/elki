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
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1].
 * <p>
 * Transformation is done using the formula
 * \(\max\{0, \mathrm{erf}(\lambda \frac{x-\mu}{\sigma\sqrt{2}})\}\)
 * <p>
 * Where mean can be fixed to a given value, and stddev is then computed against
 * this mean.
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
public class MinusLogStandardDeviationScaling extends StandardDeviationScaling {
  /**
   * Constructor.
   * 
   * @param fixedmean Fixed mean
   * @param lambda Scaling factor lambda
   */
  public MinusLogStandardDeviationScaling(double fixedmean, double lambda) {
    super(fixedmean, lambda);
  }

  @Override
  public double getScaled(double value) {
    assert (factor != 0) : "prepare() was not run prior to using the scaling function.";
    final double mlogv = -FastMath.log(value);
    return mlogv < mean || Double.isNaN(mlogv) ? 0. : //
        Math.max(0.0, NormalDistribution.erf((mlogv - mean) / factor));
  }

  @Override
  public void prepare(OutlierResult or) {
    if(Double.isNaN(fixedmean)) {
      MeanVariance mv = new MeanVariance();
      DoubleRelation scores = or.getScores();
      for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
        double val = -FastMath.log(scores.doubleValue(id));
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          mv.put(val);
        }
      }
      mean = mv.getMean();
      factor = lambda * mv.getSampleStddev() * MathUtil.SQRT2;
    }
    else {
      mean = fixedmean;
      Mean sqsum = new Mean();
      DoubleRelation scores = or.getScores();
      for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
        double val = -FastMath.log(scores.doubleValue(id));
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          sqsum.put((val - mean) * (val - mean));
        }
      }
      factor = lambda * FastMath.sqrt(sqsum.getMean()) * MathUtil.SQRT2;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends StandardDeviationScaling.Parameterizer {
    @Override
    protected MinusLogStandardDeviationScaling makeInstance() {
      return new MinusLogStandardDeviationScaling(fixedmean, lambda);
    }
  }
}
