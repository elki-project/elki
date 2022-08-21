/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.intrinsicdimensionality;

import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

import net.jafama.FastMath;

/**
 * TightLID Estimator (TLE) of the intrinsic dimensionality (maximum likelihood
 * estimator for ID using auxiliary distances).
 * <p>
 * Reference:
 * <p>
 * Laurent Amsaleg, Oussama Chelly, Michael E. Houle, Ken-ichi Kawarabayashi,
 * Milos Radovanovic, Weeris Treeratanajaru<br>
 * Intrinsic Dimensionality Estimation within Tight Localities<br>
 * Proc. 2019 SIAM International Conference on Data Mining (SDM)
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "Laurent Amsaleg, Oussama Chelly, Michael E. Houle, Ken-ichi Kawarabayashi, Milos Radovanovic, Weeris Treeratanajaru", //
    title = "Intrinsic Dimensionality Estimation within Tight Localities", //
    booktitle = "Proc. 2019 SIAM International Conference on Data Mining (SDM)", //
    url = "https://doi.org/10.1137/1.9781611975673.21", //
    bibkey = "DBLP:conf/sdm/AmsalegCHKRT19")
public class TightLIDEstimator implements IntrinsicDimensionalityEstimator<Object> {
  /**
   * Static instance.
   */
  public static final TightLIDEstimator STATIC = new TightLIDEstimator();

  @Override
  public double estimate(RangeSearcher<DBIDRef> rnq, DistanceQuery<? extends Object> distq, DBIDRef cur, double range) {
    final boolean issquared = distq.getDistance().isSquared();
    final DoubleDBIDList kl = rnq.getRange(cur, range);
    // FIXME: or use the maximum distance observed?
    final double r = range, r2 = issquared ? r : r * r;

    double sum = 0;
    // We fill the upper triangle only,
    DoubleDBIDListIter ii = kl.iter(), ij = kl.iter();
    long valid = 0;
    // Compute pairwise distances:
    for(ii.seek(0); ii.valid(); ii.advance()) {
      final double kdi = ii.doubleValue();
      if(kdi <= 0. || kdi >= r) {
        continue;
      }
      double Di2 = issquared ? kdi : kdi * kdi;
      double r2mDi2 = 2 * (r2 - Di2), ir2mDi2 = 1. / r2mDi2;
      for(ij.seek(ii.getOffset() + 1); ij.valid(); ij.advance()) {
        final double kdj = ij.doubleValue();
        if(kdj <= 0. || kdj >= r) {
          continue;
        }
        final double d = distq.distance(ii, ij);
        double Dj2 = issquared ? kdj : kdj * kdj, V2 = issquared ? d : d * d;
        // Real point:
        double S = Di2 + V2 - Dj2;
        S = (Math.sqrt(S * S + 2 * V2 * r2mDi2) - S) * ir2mDi2;
        if(S <= 0) {
          continue;
        }
        // Virtual point:
        double Z2 = 2 * Di2 + 2 * Dj2 - V2;
        double T = Di2 + Z2 - Dj2;
        T = (Math.sqrt(T * T + 2 * Z2 * r2mDi2) - T) * ir2mDi2;
        if(T > 0) {
          sum += 2 * (FastMath.log(T) + FastMath.log(S));
          valid += 2;
        }
      }
      // Square cancels out with taking this twice.
      sum += FastMath.log(Di2 / r2);
      ++valid;
    }
    // TODO: there are still some special cases missing?
    return sum < 0 ? -valid / sum * (issquared ? 2 : 1) : 1;
  }

  @Override
  public double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<? extends Object> distq, DBIDRef cur, int k) {
    final boolean issquared = distq.getDistance().isSquared();
    final KNNList kl = knnq.getKNN(cur, k);
    final double r = kl.getKNNDistance(), r2 = issquared ? r : r * r;

    double sum = 0;
    // We fill the upper triangle only,
    DoubleDBIDListIter ii = kl.iter(), ij = kl.iter();
    long valid = 0;
    // Compute pairwise distances:
    for(ii.seek(0); ii.valid(); ii.advance()) {
      final double kdi = ii.doubleValue();
      if(kdi <= 0. || kdi >= r) {
        continue;
      }
      double Di2 = issquared ? kdi : kdi * kdi;
      double r2mDi2 = 2 * (r2 - Di2), ir2mDi2 = 1. / r2mDi2;
      for(ij.seek(ii.getOffset() + 1); ij.valid(); ij.advance()) {
        final double kdj = ij.doubleValue();
        if(kdj <= 0. || kdj >= r) {
          continue;
        }
        final double d = distq.distance(ii, ij);
        double Dj2 = issquared ? kdj : kdj * kdj, V2 = issquared ? d : d * d;
        // Real point:
        double S = Di2 + V2 - Dj2;
        S = (Math.sqrt(S * S + 2 * V2 * r2mDi2) - S) * ir2mDi2;
        if(S <= 0) {
          continue;
        }
        // Virtual point:
        double Z2 = 2 * Di2 + 2 * Dj2 - V2;
        double T = Di2 + Z2 - Dj2;
        T = (Math.sqrt(T * T + 2 * Z2 * r2mDi2) - T) * ir2mDi2;
        if(T > 0) {
          sum += 2 * (FastMath.log(T) + FastMath.log(S));
          valid += 2;
        }
      }
      // Square cancels out with taking this twice.
      sum += FastMath.log(Di2 / r2);
      ++valid;
    }
    // TODO: there are still some special cases missing?
    return sum < 0 ? -valid / sum * (issquared ? 2 : 1) : 1;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public TightLIDEstimator make() {
      return STATIC;
    }
  }
}
