package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;

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

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Abstract base class for ID estimators.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractIntrinsicDimensionalityEstimator implements IntrinsicDimensionalityEstimator {
  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter) {
    return estimate(data, adapter, adapter.size(data));
  }

  @Override
  public double estimate(double[] distances) {
    return estimate(distances, ArrayLikeUtil.DOUBLEARRAYADAPTER, distances.length);
  }

  @Override
  public double estimate(double[] distances, int size) {
    return estimate(distances, ArrayLikeUtil.DOUBLEARRAYADAPTER, size);
  }

  @Override
  public double estimate(KNNQuery<?> knnq, DBIDRef cur, int k) {
    double[] buf = new double[k];
    int p = 0;
    for(DoubleDBIDListIter it = knnq.getKNNForDBID(cur, k).iter(); it.valid() && p < k; it.advance()) {
      if(it.doubleValue() == 0. || DBIDUtil.equal(cur, it)) {
        continue;
      }
      buf[p++] = it.doubleValue();
    }
    return estimate(buf, ArrayLikeUtil.DOUBLEARRAYADAPTER, p);
  }

  @Override
  public double estimate(RangeQuery<?> rnq, DBIDRef cur, double range) {
    DoubleArray buf = new DoubleArray();
    int p = 0;
    for(DoubleDBIDListIter it = rnq.getRangeForDBID(cur, range).iter(); it.valid(); it.advance()) {
      if(it.doubleValue() == 0. || DBIDUtil.equal(cur, it)) {
        continue;
      }
      buf.add(it.doubleValue());
      p++;
    }
    return estimate(buf, buf, p);
  }

  /**
   * @param data Data array
   * @param adapter Adapter class
   * @param end Length
   * @return Number of leading zero distances.
   */
  protected static <A> int countLeadingZeros(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    for(int begin = 0; begin < end; ++begin) {
      if(adapter.getDouble(data, begin) > 0) {
        return begin;
      }
    }
    return end;
  }
}
