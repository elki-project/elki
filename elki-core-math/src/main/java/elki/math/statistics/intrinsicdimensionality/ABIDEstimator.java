/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

import net.jafama.FastMath;

/**
 * Angle based intrinsic dimensionality (ABID) estimator.
 *
 * @author Erik Thordsen
 */
public class ABIDEstimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final ABIDEstimator STATIC = new ABIDEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, int size) {
    throw new UnsupportedOperationException("The ABIDEstimator can only be used with neighbour queries.");
  }

  /* Squared cosine from squared triangle side lengths */
  private final double cos2(final double sideA2, final double sideB2, final double oppositeSide2) {
    final double numerator = sideA2 + sideB2 - oppositeSide2;
    return numerator * numerator / (4 * sideA2 * sideB2);
  }

  @Override
  public double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<?> distq, DBIDRef cur, int k) {
    final boolean issquared = distq.getDistance().isSquared();
    final KNNList kl = knnq.getKNN(cur, k);
    /* Removing the query point from k. */
    k -= 1;

    double ssq = 0;
    // We fill the upper triangle only,
    final DoubleDBIDListIter ii = kl.iter();
    final DoubleDBIDListIter ij = kl.iter();
    /* Compute squared cosines */
    /* Offset by 1 to avoid the point itself */
    for (ii.seek(1); ii.valid(); ii.advance()) {
      final double kdi = ii.doubleValue();
      final double Di2 = issquared ? kdi : kdi * kdi;
      if(Di2 == 0) {
        continue;
      }
      for (ij.seek(ii.getOffset() + 1); ij.valid(); ij.advance()) {
        final double kdj = ij.doubleValue();
        final double Dj2 = issquared ? kdj : kdj * kdj;
        final double Vh = distq.distance(ii, ij);
        final double V2 = issquared ? Vh : Vh * Vh;
        ssq += cos2(Di2, Dj2, V2);
      }
    }
    /* Times two for lower half and plus k for diagonal. */
    ssq = 2*ssq + k;
    return k*k/ssq;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public ABIDEstimator make() {
      return STATIC;
    }
  }
}
