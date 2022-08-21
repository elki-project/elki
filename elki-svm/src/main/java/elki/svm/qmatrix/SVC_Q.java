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

import elki.svm.CSVC;
import elki.svm.NuSVC;
import elki.svm.data.DataSet;
import elki.utilities.datastructures.arrays.ArrayUtil;

/**
 * Q matrix used by {@link CSVC} and {@link NuSVC} classification.
 */
public class SVC_Q extends Kernel {
  private byte[] y;

  public SVC_Q(DataSet x, final byte[] y) {
    super(x);
    this.y = y.clone(); // Because we reorder y.
  }

  @Override
  public double similarity(int i, int j) {
    return y[i] * y[j] * x.similarity(i, j);
  }

  @Override
  public void swap_index(int i, int j) {
    if(i == j) {
      return;
    }
    super.swap_index(i, j);
    ArrayUtil.swap(y, i, j);
  }
}
