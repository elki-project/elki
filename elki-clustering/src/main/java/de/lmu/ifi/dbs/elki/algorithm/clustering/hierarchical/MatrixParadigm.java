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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Shared code for algorithms that work on a strict matrix paradigm.
 *
 * Note that this requires \(O(n^2)\) memory (and often \(O(n^3)\) runtime).
 *
 * This class bridges the gap from the relational (indexed by identifiers) and
 * the matrix view (indexed by integers 0...n-1).
 *
 * While this will usually store (merge-) distances when clustering, it can
 * store arbitrary doubles.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MatrixParadigm {
  /**
   * Two iterators to reference to objects.
   */
  public final DBIDArrayIter ix, iy;

  /**
   * Distance matrix (<b>modifiable</b>).
   */
  public final double[] matrix;

  /**
   * Number of rows/columns.
   */
  public final int size;

  /**
   * Constructor.
   *
   * @param ids Database ids.
   */
  public MatrixParadigm(DBIDs ids) {
    size = ids.size();
    if(size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + //
          0x10000 // = 65535
          + " instances (~16 GB RAM), at which point the Java maximum array size is reached.");
    }
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    ix = aids.iter();
    iy = aids.iter();
    matrix = new double[triangleSize(size)];
  }

  /**
   * Compute the size of a complete x by x triangle (minus diagonal)
   *
   * @param x Offset
   * @return Size of complete triangle
   */
  public static int triangleSize(int x) {
    return (x * (x - 1)) >>> 1;
  }

  /**
   * Get a value from the (upper triangular) distance matrix.
   *
   * Note: in many cases, linear iteration over the matrix will be fastet than
   * repeated calls to this method!
   *
   * @param x First object
   * @param y Second object
   * @return Distance
   */
  public double get(int x, int y) {
    return (x == y) ? 0 : (x < y) //
        ? matrix[MatrixParadigm.triangleSize(y) + x] //
        : matrix[MatrixParadigm.triangleSize(x) + y];
  }

  /**
   * Initialize a distance matrix.
   *
   * @param dq Distance query
   * @return this
   */
  public MatrixParadigm initializeWithDistances(DistanceQuery<?> dq) {
    final DBIDArrayIter ix = this.ix, iy = this.iy;
    final double[] matrix = this.matrix;
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      final int x = ix.getOffset();
      assert (pos == triangleSize(x));
      for(iy.seek(0); iy.getOffset() < x; iy.advance()) {
        matrix[pos++] = dq.distance(ix, iy);
      }
    }
    return this;
  }
}
