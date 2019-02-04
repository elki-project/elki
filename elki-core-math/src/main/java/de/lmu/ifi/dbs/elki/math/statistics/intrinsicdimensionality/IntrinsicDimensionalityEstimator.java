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
package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Estimate the intrinsic dimensionality from a distance list.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface IntrinsicDimensionalityEstimator {
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

  /**
   * Estimate from a Reference Point, a KNNQuery and the neighborhood size k.
   * 
   * @param knnq KNNQuery
   * @param cur reference point
   * @param k neighborhood size
   * @return Estimated intrinsic dimensionality
   */
  default double estimate(KNNQuery<?> knnq, DBIDRef cur, int k) {
    double[] buf = new double[k];
    int p = 0;
    for(DoubleDBIDListIter it = knnq.getKNNForDBID(cur, k).iter(); it.valid() && p < k; it.advance()) {
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

  /**
   * Estimate from a distance list.
   * 
   * @param rnq RangeQuery
   * @param cur reference point
   * @param range neighborhood radius
   * @return Estimated intrinsic dimensionality
   */
  default double estimate(RangeQuery<?> rnq, DBIDRef cur, double range) {
    DoubleArray buf = new DoubleArray();
    int p = 0;
    for(DoubleDBIDListIter it = rnq.getRangeForDBID(cur, range).iter(); it.valid(); it.advance()) {
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
