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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Generalized Expansion Dimension for estimating the intrinsic dimensionality.
 * <p>
 * Reference:
 * <p>
 * M. E. Houle, H. Kashima, M. Nett<br>
 * Generalized expansion dimension<br>
 * In: 12th International Conference on Data Mining Workshops (ICDMW)
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "M. E. Houle, H. Kashima, M. Nett", //
    title = "Generalized expansion dimension", //
    booktitle = "12th International Conference on Data Mining Workshops (ICDMW)", //
    url = "https://doi.org/10.1109/ICDMW.2012.94", //
    bibkey = "DBLP:conf/icdm/HouleKN12")
public class GEDEstimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final GEDEstimator STATIC = new GEDEstimator();

  /**
   * Cached logs of integers.
   */
  double[] ilogs = new double[] { 0. };

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int begin = IntrinsicDimensionalityEstimator.countLeadingZeros(data, adapter, end);
    if(end - begin < 2) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    final int last = end - begin - 1;
    double[] meds = new double[last];
    if(last >= ilogs.length) { // Unsynchronized check
      precomputeLogs(last + 1); // Synchronized resize
    }
    // We only consider pairs with k < i, to avoid redundant computations.
    for(int k = 0; k < last; k++) {
      final double logdk = FastMath.log(adapter.getDouble(data, begin + k));
      double log1pk = ilogs[k];
      int p = k; // k values are already occupied!
      // We only consider pairs with k < i, to avoid redundant computations.
      for(int i = k + 1; i <= last; i++) {
        final double logdi = FastMath.log(adapter.getDouble(data, begin + i));
        if(logdk == logdi) { // Would yield a division by 0.
          continue;
        }
        meds[p++] = (log1pk - ilogs[i]) / (logdk - logdi);
      }
      meds[k] = QuickSelect.median(meds, k, p);
    }
    return QuickSelect.median(meds, 0, last);
  }

  /**
   * Grow the log[i] cache.
   * 
   * @param len Required size
   */
  private synchronized void precomputeLogs(int len) {
    if(len <= ilogs.length) {
      return; // Probably done by another thread.
    }
    double[] logs = Arrays.copyOf(ilogs, len);
    for(int i = ilogs.length; i < len; i++) {
      logs[i] = FastMath.log1p(i);
    }
    this.ilogs = logs;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GEDEstimator makeInstance() {
      return STATIC;
    }
  }
}
