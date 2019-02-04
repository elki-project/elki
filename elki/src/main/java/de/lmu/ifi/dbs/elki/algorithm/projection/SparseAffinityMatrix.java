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
package de.lmu.ifi.dbs.elki.algorithm.projection;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Dense affinity matrix storage.
 * 
 * TODO: it is likely faster to store this in a 1-d array when possible.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SparseAffinityMatrix implements AffinityMatrix {
  /**
   * Non-zero entries
   */
  double[][] pij;

  /**
   * Non-zero indexes
   */
  int[][] indices;

  /**
   * Indexed data points
   */
  ArrayDBIDs ids;

  public SparseAffinityMatrix(double[][] pij, int[][] indices, ArrayDBIDs ids) {
    this.pij = pij;
    this.indices = indices;
    this.ids = ids;
  }

  @Override
  public double get(int i, int j) {
    // FIXME: ensure these arrrays are sorted, then use binary search.
    int[] ind = indices[i];
    for(int y = 0; y < ind.length; y++) {
      if(ind[y] == j) {
        return pij[i][y];
      }
    }
    return 0;
  }

  @Override
  public void scale(double d) {
    VMath.timesEquals(pij, d);
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public DBIDArrayIter iterDBIDs() {
    return ids.iter();
  }

  @Override
  public int iter(int x) {
    return 0;
  }

  @Override
  public int iterAdvance(int x, int iter) {
    return iter + 1;
  }

  @Override
  public int iterDim(int x, int iter) {
    return indices[x][iter];
  }

  @Override
  public double iterValue(int x, int iter) {
    return pij[x][iter];
  }

  @Override
  public boolean iterValid(int x, int iter) {
    return iter < pij[x].length;
  }
}