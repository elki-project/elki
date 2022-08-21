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
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Raw angle based intrinsic dimensionality (RABID) estimator.
 * <p>
 * Reference:
 * <p>
 * Erik Thordsen and Erich Schubert<br>
 * ABID: Angle Based Intrinsic Dimensionality<br>
 * Proc. 13th Int. Conf. Similarity Search and Applications (SISAP'2020)
 *
 * @author Erik Thordsen
 * @since 0.8.0
 */
@Reference(authors = "Erik Thordsen and Erich Schubert", //
    title = "ABID: Angle Based Intrinsic Dimensionality", //
    booktitle = "Proc. 13th Int. Conf. Similarity Search and Applications (SISAP'2020)", //
    url = "https://doi.org/10.1007/978-3-030-60936-8_17", //
    bibkey = "DBLP:conf/sisap/ThordsenS20")
public class RABIDEstimator implements DistanceBasedIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final RABIDEstimator STATIC = new RABIDEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, int size) {
    throw new UnsupportedOperationException("The RABIDEstimator can only be used with neighbour queries.");
  }

  /**
   * Squared cosine from squared triangle side lengths
   * 
   * @param sideA2 squared first side
   * @param sideB2 squared second side
   * @param oppositeSide2 squared opposite side
   * @return Squared cosine
   */
  private final double cos2(double sideA2, double sideB2, double oppositeSide2) {
    final double numerator = sideA2 + sideB2 - oppositeSide2;
    return numerator * numerator / (4 * sideA2 * sideB2);
  }

  @Override
  public double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<?> distq, DBIDRef cur, int k) {
    return computeABID(distq, knnq.getKNN(cur, k), false /* RABID: false */);
  }

  /**
   * Estimate intrinsic dimensionality (both variants).
   *
   * @param distq Distance query
   * @param knn k nearest neighbors (usually +1, as the query point is included)
   * @param bias true to use ABID, false for RABID
   * @return intrinsic dimensionality
   */
  protected double computeABID(DistanceQuery<?> distq, KNNList knn, boolean bias) {
    final boolean issquared = distq.getDistance().isSquared();
    // Compute the upper triangle of squared cosines only.
    double ssq = 0;
    DoubleDBIDListIter ij = knn.iter();
    int k = 0;
    for(DoubleDBIDListIter ii = knn.iter(); ii.valid(); ii.advance()) {
      final double kdi = ii.doubleValue();
      if(kdi <= 0) {
        continue;
      }
      k++; // Usable neighbor
      final double Di2 = issquared ? kdi : kdi * kdi;
      for(ij.seek(ii.getOffset() + 1); ij.valid(); ij.advance()) {
        final double kdj = ij.doubleValue();
        final double Dj2 = issquared ? kdj : kdj * kdj;
        final double Vh = distq.distance(ii, ij);
        final double V2 = issquared ? Vh : Vh * Vh;
        ssq += cos2(Di2, Dj2, V2);
      }
    }
    // We only computed half of the matrix, and no diagonal:
    ssq = 2 * ssq + (bias ? k : 0);
    return k > 0 ? k * k / ssq : Double.NaN;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public RABIDEstimator make() {
      return STATIC;
    }
  }
}
