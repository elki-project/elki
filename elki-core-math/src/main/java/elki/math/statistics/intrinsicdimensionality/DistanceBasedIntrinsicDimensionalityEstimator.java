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
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.utilities.datastructures.arraylike.DoubleArray;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Distance-based ID estimator.
 * <p>
 * This is the common case, but we have some estimators that require either the
 * ability to query for neighbors of additional points, or to access the
 * coordinate data.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface DistanceBasedIntrinsicDimensionalityEstimator extends IntrinsicDimensionalityEstimator<Object> {
  /**
   * Estimate from a distance list.
   * 
   * @param data Data
   * @param adapter Array adapter
   * @param size Length
   * @param <A> array type
   * @return Estimated intrinsic dimensionality
   */
  <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, int size);

  /**
   * Estimate from a distance list.
   * 
   * @param distances Distances
   * @return Estimated intrinsic dimensionality
   */
  default double estimate(double[] distances) {
    return estimate(distances, DoubleArrayAdapter.STATIC, distances.length);
  }

  /**
   * Estimate from a distance list.
   * 
   * @param distances Distances
   * @param size Valid size
   * @return Estimated intrinsic dimensionality
   */
  default double estimate(double[] distances, int size) {
    return estimate(distances, DoubleArrayAdapter.STATIC, size);
  }

  /**
   * Estimate from a distance list.
   * 
   * @param data Data
   * @param adapter Array adapter
   * @param <A> array type
   * @return Estimated intrinsic dimensionality
   */
  default <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter) {
    return estimate(data, adapter, adapter.size(data));
  }

  @Override
  default double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<? extends Object> distq, DBIDRef cur, int k) {
    double[] buf = new double[k];
    int p = 0;
    for(DoubleDBIDListIter it = knnq.getKNN(cur, k).iter(); it.valid() && p < k; it.advance()) {
      if(it.doubleValue() == 0. || DBIDUtil.equal(cur, it)) {
        continue;
      }
      buf[p++] = it.doubleValue();
    }
    if(p < 1) {
      throw new ArithmeticException("ID estimation requires non-zero distances.");
    }
    return estimate(buf, DoubleArrayAdapter.STATIC, p);
  }

  @Override
  default double estimate(RangeSearcher<DBIDRef> rnq, DistanceQuery<? extends Object> distq, DBIDRef cur, double range) {
    final DoubleDBIDList q = rnq.getRange(cur, range);
    DoubleArray buf = new DoubleArray(q.size());
    int p = 0;
    for(DoubleDBIDListIter it = q.iter(); it.valid(); it.advance()) {
      if(it.doubleValue() == 0. || DBIDUtil.equal(cur, it)) {
        continue;
      }
      buf.add(it.doubleValue());
      p++;
    }
    if(p < 1) {
      throw new ArithmeticException("ID estimation requires non-zero distances.");
    }
    return estimate(buf, buf, p);
  }

  /**
   * @param data Data array
   * @param adapter Adapter class
   * @param end Length
   * @return Number of leading zero distances.
   */
  static <A> int countLeadingZeros(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    for(int begin = 0; begin < end; ++begin) {
      if(adapter.getDouble(data, begin) > 0) {
        return begin;
      }
    }
    return end;
  }
}
