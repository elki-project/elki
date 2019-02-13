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
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * ALID estimator of the intrinsic dimensionality (maximum likelihood estimator
 * for ID using auxiliary distances).
 * <p>
 * Reference:
 * <p>
 * Oussama Chelly, Michael E. Houle, Ken-ichi Kawarabayashi<br>
 * Enhanced Estimation of Local Intrinsic Dimensionality Using Auxiliary
 * Distances<br>
 * Contributed to ELKI
 *
 * @author Jonathan von Brünken
 * @since 0.7.5
 */
@Reference(authors = "Oussama Chelly, Michael E. Houle, Ken-ichi Kawarabayashi", //
    title = "Enhanced Estimation of Local Intrinsic Dimensionality Using Auxiliary Distances", //
    booktitle = "Contributed to ELKI", //
    bibkey = "tr/nii/ChellyHK16")
public class ALIDEstimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final ALIDEstimator STATIC = new ALIDEstimator();

  @Override
  public double estimate(KNNQuery<?> knnq, DBIDRef cur, int k) {
    int a = 0;
    double sum = 0;
    final KNNList kl = knnq.getKNNForDBID(cur, k);
    final double w = kl.getKNNDistance();
    final double halfw = 0.5 * w;
    for(DoubleDBIDListIter it = kl.iter(); it.valid(); it.advance()) {
      if(it.doubleValue() <= 0. || DBIDUtil.equal(cur, it)) {
        continue;
      }
      final double v = it.doubleValue();
      sum += v < halfw ? FastMath.log(v / w) : FastMath.log1p((v - w) / w);
      ++a;
      final double nw = w - v;
      final double halfnw = 0.5 * nw;
      for(DoubleDBIDListIter it2 = knnq.getKNNForDBID(it, k).iter(); it2.valid() && it2.doubleValue() <= nw; it2.advance()) {
        if(it2.doubleValue() <= 0. || DBIDUtil.equal(it, it2)) {
          continue;
        }
        final double v2 = it2.doubleValue();
        sum += v2 < halfnw ? FastMath.log(v2 / nw) : FastMath.log1p((v2 - nw) / nw);
        ++a;
      }
    }
    return -a / sum;
  }

  @Override
  public double estimate(RangeQuery<?> rnq, DBIDRef cur, double range) {
    int a = 0;
    double sum = 0;
    final double halfw = 0.5 * range;
    for(DoubleDBIDListIter it = rnq.getRangeForDBID(cur, range).iter(); it.valid(); it.advance()) {
      if(it.doubleValue() == 0. || DBIDUtil.equal(cur, it)) {
        continue;
      }
      final double v = it.doubleValue();
      sum += v < halfw ? FastMath.log(v / range) : FastMath.log1p((v - range) / range);
      ++a;
      final double nw = range - v;
      final double halfnw = 0.5 * nw;
      for(DoubleDBIDListIter it2 = rnq.getRangeForDBID(it, nw).iter(); it.valid(); it.advance()) {
        if(it2.doubleValue() <= 0. || DBIDUtil.equal(it, it2)) {
          continue;
        }
        final double v2 = it2.doubleValue();
        sum += v2 < halfnw ? FastMath.log(v2 / nw) : FastMath.log1p((v2 - nw) / nw);
        ++a;
      }
    }
    return -a / sum;
  }

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, int size) {
    throw new UnsupportedOperationException("The ALID estimator can only be used with neighbor queries.");
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ALIDEstimator makeInstance() {
      return STATIC;
    }
  }
}
