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

import java.util.Arrays;

import elki.svm.EpsilonSVR;
import elki.svm.NuSVR;
import elki.svm.data.DataSet;
import elki.utilities.datastructures.arrays.ArrayUtil;

/**
 * Q matrix used for regression by {@link EpsilonSVR} and {@link NuSVR}.
 * <p>
 * This uses two "copies" of the data, one for upper bounding and one for lower
 * bounding of the data, yielding a virtual size of 2*l.
 * <p>
 * To save memory, we cache inside, to avoid duplication.
 */
public class SVR_Q implements QMatrix {
  private final int l;

  private final byte[] sign;

  private final int[] index;

  private CachedQMatrix inner;

  private final double[] QD;

  public SVR_Q(DataSet x, double cache_size) {
    super();
    this.l = x.size();
    this.inner = new CachedQMatrix(l, cache_size, new Kernel(x));
    final int l2 = l << 1;
    sign = new byte[l2];
    Arrays.fill(sign, 0, l, (byte) 1);
    Arrays.fill(sign, l, l2, (byte) -1);
    index = new int[l2];
    for(int k = 0, k2 = l; k < l; k++, k2++) {
      index[k] = index[k2] = k;
    }
    this.QD = new double[l2];
  }

  @Override
  public void initialize() {
    // This would initialize the inner QD: inner.initialize();
    // Initialize our QD.
    double[] QD = this.QD;
    for(int k = 0, k2 = l; k < l; k++, k2++) {
      QD[k2] = QD[k] = similarity(k, k);
    }
  }

  @Override
  public double[] get_QD() {
    return QD;
  }

  @Override
  public void swap_index(int i, int j) {
    // Note: the inner matrix is not swapped!
    // Instead, we use 'index' as indirection array.
    ArrayUtil.swap(sign, i, j);
    ArrayUtil.swap(index, i, j);
    ArrayUtil.swap(QD, i, j);
  }

  @Override
  public void get_Q(int i, int len, float[] out) {
    final int real_i = index[i];
    // From cache, not reordered; always get all l values!
    float[] data = inner.get_data(real_i, l);

    // reorder and copy to output
    final byte si = sign[i];
    for(int j = 0; j < len; j++) {
      out[j] = (float) si * sign[j] * data[index[j]];
    }
  }

  @Override
  public double similarity(int i, int j) {
    return inner.similarity(index[i], index[j]);
  }
}
