package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Generalized Expansion Dimension for estimating the intrinsic dimensionality.
 *
 * Reference:
 * <p>
 * M. E. Houle, H. Kashima, M. Nett<br />
 * Generalized expansion dimension<br />
 * In: 12th International Conference on Data Mining Workshops (ICDMW)
 * </p>
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "M. E. Houle, H. Kashima, M. Nett", //
title = "Generalized expansion dimension", //
booktitle = "12th International Conference on Data Mining Workshops (ICDMW)", //
url = "http://dx.doi.org/10.1109/ICDMW.2012.94")
public class GEDEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final GEDEstimator STATIC = new GEDEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter, final int len) {
    if(len < 2) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    final int end = len - 1;
    double[] meds = new double[end << 1];
    // We only consider pairs with k < i, to avoid redundant computations.
    for(int k = 0; k < end; k++) {
      final double logdk = Math.log(adapter.getDouble(data, k));
      double log1pk = Math.log1p(k);
      int p = k; // k values are already occupied!
      // We only consider pairs with k < i, to avoid redundant computations.
      for(int i = k + 1; i < len; i++) {
        final double logdi = Math.log(adapter.getDouble(data, i));
        if(logdk == logdi) { // Would yield a division by 0.
          continue;
        }
        final double dim = (log1pk - Math.log1p(i)) / (logdk - logdi);
        meds[p++] = dim;
      }
      meds[k] = QuickSelect.median(meds, k, p);
    }
    return QuickSelect.median(meds, 0, end);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GEDEstimator makeInstance() {
      return STATIC;
    }
  }
}
