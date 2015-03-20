package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    if(len < 2) {
      throw new ArithmeticException("Cannot compute expansion dimensionality for a single observation.");
    }
    double[] meds = new double[(len << 1) - 1];
    for(int r = 0; r < len; r++) {
      final double d1 = adapter.getDouble(data, r);
      int p = r;
      for(int r2 = 0; r2 < len; r2++) {
        if(r == r2) {
          continue;
        }
        final double d2 = adapter.getDouble(data, r2);
        final double dim = Math.log((r + 1.) / (r2 + 1.)) / Math.log(d1 / d2);
        meds[p++] = dim;
      }
      meds[r] = QuickSelect.median(meds, r, p);
    }
    return QuickSelect.median(meds, 0, len);
  }
}
