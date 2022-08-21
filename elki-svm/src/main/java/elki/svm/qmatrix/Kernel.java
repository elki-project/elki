/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.svm.qmatrix;

import elki.svm.data.DataSet;
import elki.utilities.datastructures.arrays.ArrayUtil;

public class Kernel implements QMatrix {
  protected final DataSet x;

  // Diagonal values <x,x>
  private final double[] QD;

  public Kernel(DataSet x) {
    super();
    this.x = x;
    this.QD = new double[x.size()];
  }

  // TODO: move into cache instead?
  @Override
  public void initialize() {
    final int l = x.size();
    final double[] QD = this.QD;
    for(int i = 0; i < l; i++) {
      QD[i] = similarity(i, i);
    }
  }

  @Override
  public double similarity(int i, int j) {
    return x.similarity(i, j);
  }

  @Override
  public void swap_index(int i, int j) {
    if(i == j) {
      return;
    }
    x.swap(i, j);
    ArrayUtil.swap(QD, i, j);
  }

  @Override
  public final double[] get_QD() {
    return QD;
  }
}
