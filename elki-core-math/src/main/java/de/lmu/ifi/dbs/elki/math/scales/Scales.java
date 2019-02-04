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
package de.lmu.ifi.dbs.elki.math.scales;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

/**
 * Scales helper class. Currently, this will just compute a linear scale for
 * each axis. It is planned to add functionality to include some analysis to be
 * able to automatically choose log scales when appropriate.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @navhas - computes - LinearScale
 */
public final class Scales {
  /**
   * Fake constructor.
   */
  private Scales() {
    // Do not instantiate.
  }

  /**
   * Compute a linear scale for each dimension.
   *
   * @param rel Relation
   * @return Scales, indexed starting with 0 (like Vector, not database
   *         objects!)
   */
  public static LinearScale[] calcScales(Relation<? extends SpatialComparable> rel) {
    int dim = RelationUtil.dimensionality(rel);
    DoubleMinMax[] minmax = DoubleMinMax.newArray(dim);
    LinearScale[] scales = new LinearScale[dim];

    // analyze data
    for(DBIDIter iditer = rel.iterDBIDs(); iditer.valid(); iditer.advance()) {
      SpatialComparable v = rel.get(iditer);
      if(v instanceof NumberVector) {
        for(int d = 0; d < dim; d++) {
          final double mi = v.getMin(d);
          if(mi != mi) { // NaN
            continue;
          }
          minmax[d].put(mi);
        }
      }
      else {
        for(int d = 0; d < dim; d++) {
          final double mi = v.getMin(d);
          if(mi == mi) { // No NaN
            minmax[d].put(mi);
          }
          final double ma = v.getMax(d);
          if(ma == ma) { // No NaN
            minmax[d].put(ma);
          }
        }
      }
    }

    // generate scales
    for(int d = 0; d < dim; d++) {
      scales[d] = new LinearScale(minmax[d].getMin(), minmax[d].getMax());
    }
    return scales;
  }
}
