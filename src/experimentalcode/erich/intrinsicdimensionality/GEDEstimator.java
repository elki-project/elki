package experimentalcode.erich.intrinsicdimensionality;

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

/**
 * Generalized Expansion Dimension for estimating the intrinsic dimensionality.
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 */
public class GEDEstimator implements IntrinsicDimensionalityEstimator {
  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
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
