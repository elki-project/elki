package de.lmu.ifi.dbs.elki.math.scales;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Scales helper class. Currently, this will just compute a linear scale for
 * each axis. It is planned to add functionality to include some analysis to be
 * able to automatically choose log scales when appropriate.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has LinearScale oneway - - computes
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
   * @param <O> vector type
   * @param db Database
   * @return Scales, indexed starting with 0 (like Vector, not database
   *         objects!)
   */
  public static <O extends NumberVector<? extends Number>> LinearScale[] calcScales(Relation<O> db) {
    if (db == null) {
      throw new AbortException("No database was given to Scales.calcScales.");
    }
    int dim = RelationUtil.dimensionality(db);
    DoubleMinMax[] minmax = DoubleMinMax.newArray(dim);
    LinearScale[] scales = new LinearScale[dim];

    // analyze data
    for (DBIDIter iditer = db.iterDBIDs(); iditer.valid(); iditer.advance()) {
      O v = db.get(iditer);
      for (int d = 0; d < dim; d++) {
        minmax[d].put(v.doubleValue(d));
      }
    }

    // generate scales
    for (int d = 0; d < dim; d++) {
      scales[d] = new LinearScale(minmax[d].getMin(), minmax[d].getMax());
    }
    return scales;
  }
}
