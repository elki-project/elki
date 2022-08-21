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
package elki.svm;

import java.util.Arrays;
import java.util.Random;

import elki.logging.Logging;
import elki.svm.data.DataSet;
import elki.svm.solver.Solver;
import elki.utilities.datastructures.arrays.ArrayUtil;

public abstract class AbstractSingleSVM {
  protected static final byte ONE = 1, MONE = -1;

  protected double eps;

  protected boolean shrinking;

  protected double cache_size;

  public AbstractSingleSVM(double eps, boolean shrinking, double cache_size) {
    this.eps = eps;
    this.shrinking = shrinking;
    this.cache_size = cache_size;
  }

  abstract protected Solver.SolutionInfo solve(DataSet x);

  protected Solver.SolutionInfo train_one(DataSet x) {
    Solver.SolutionInfo si = solve(x);

    if(getLogger().isDebugging()) {
      StringBuilder buf = new StringBuilder(100) //
          .append("obj = ").append(si.obj) //
          .append(", rho = ").append(si.rho).append("\n");

      // output SV counts
      int nSV = 0, nBSV = 0;
      for(int i = 0, l = x.size(); i < l; i++) {
        double a_i = Math.abs(si.alpha[i]);
        if(a_i > 0) {
          ++nSV;
          if(a_i >= ((x.value(i) > 0) ? si.upper_bound_p : si.upper_bound_n)) {
            ++nBSV;
          }
        }
      }
      getLogger().debug(buf.append("nSV = ").append(nSV) //
          .append(", nBSV = ").append(nBSV).toString());
    }
    return si;
  }

  public void set_weights(double Cp, double Cn) {
    // Ignore by default.
  }

  /**
   * Build a shuffled index array.
   * 
   * @param perm
   *        Array storing the permutation
   * @param l
   *        Size
   */
  public static int[] shuffledIndex(int[] perm, int l) {
    Random rand = new Random();
    // Shuffle data set.
    for(int i = 0; i < l; i++) {
      perm[i] = i;
    }
    for(int i = 0; i < l; i++) {
      int j = i + rand.nextInt(l - i);
      ArrayUtil.swap(perm, i, j);
    }
    return perm;
  }

  public static int[] makeFolds(final int l, int nr_fold) {
    int[] fold_start = new int[nr_fold + 1];
    for(int i = 0; i <= nr_fold; i++) {
      fold_start[i] = i * l / nr_fold;
    }
    return fold_start;
  }

  public static int[] stratifiedFolds(DataSet x, int nr_fold, int[] perm) {
    final int l = x.size();
    int[] fold_start = new int[nr_fold + 1];

    int[][] group_ret = new int[3][];
    int nr_class = groupClasses(x, group_ret, perm);
    int[] start = group_ret[1], count = group_ret[2];

    Random rand = new Random();
    // random shuffle and then data grouped by fold using the array perm
    int[] fold_count = new int[nr_fold];
    int[] index = new int[l];
    for(int i = 0; i < l; i++) {
      index[i] = perm[i];
    }
    for(int c = 0; c < nr_class; c++) {
      for(int i = 0; i < count[c]; i++) {
        int j = i + rand.nextInt(count[c] - i);
        ArrayUtil.swap(index, start[c] + i, start[c] + j);
      }
    }
    for(int i = 0; i < nr_fold; i++) {
      fold_count[i] = 0;
      for(int c = 0; c < nr_class; c++) {
        fold_count[i] += (i + 1) * count[c] / nr_fold - i * count[c] / nr_fold;
      }
    }
    fold_start[0] = 0;
    for(int i = 1; i <= nr_fold; i++) {
      fold_start[i] = fold_start[i - 1] + fold_count[i - 1];
    }
    for(int c = 0; c < nr_class; c++) {
      for(int i = 0; i < nr_fold; i++) {
        int begin = start[c] + i * count[c] / nr_fold;
        int end = start[c] + (i + 1) * count[c] / nr_fold;
        for(int j = begin; j < end; j++) {
          perm[fold_start[i]] = index[j];
          fold_start[i]++;
        }
      }
    }
    fold_start[0] = 0;
    for(int i = 1; i <= nr_fold; i++) {
      fold_start[i] = fold_start[i - 1] + fold_count[i - 1];
    }
    return fold_start;
  }

  // label: label name, start: begin of each class, count: #data of classes,
  // perm: indices to the original data
  // perm, length l, must be allocated before calling this subroutine
  protected static int groupClasses(DataSet x, int[][] group_ret, int[] perm) {
    final int l = x.size();
    final int max_nr_class = 16; // Initial allocation
    int[] label = new int[max_nr_class];
    int[] count = new int[max_nr_class];
    int[] data_label = new int[l];

    int nr_class = 0;
    for(int i = 0; i < l; i++) {
      int this_label = x.classnum(i);
      int j;
      for(j = 0; j < nr_class; j++) {
        if(this_label == label[j]) {
          ++count[j];
          break;
        }
      }
      data_label[i] = j;
      if(j == nr_class) {
        // Resize when necessary
        if(nr_class == label.length) {
          label = Arrays.copyOf(label, label.length << 1);
          count = Arrays.copyOf(count, count.length << 1);
        }
        label[nr_class] = this_label;
        count[nr_class] = 1;
        ++nr_class;
      }
    }

    //
    // Labels are ordered by their first occurrence in the training set.
    // However, for two-class sets with -1/+1 labels and -1 appears first,
    // we swap labels to ensure that internally the binary SVM has positive
    // data corresponding to the +1 instances.
    //
    if(nr_class == 2 && label[0] == -1 && label[1] == +1) {
      ArrayUtil.swap(label, 0, 1);
      ArrayUtil.swap(count, 0, 1);
      for(int i = 0; i < l; i++) {
        data_label[i] = (data_label[i] == 0) ? 1 : 0;
      }
    }

    int[] start = new int[nr_class];
    start[0] = 0;
    for(int i = 1; i < nr_class; i++) {
      start[i] = start[i - 1] + count[i - 1];
    }
    for(int i = 0; i < l; i++) {
      perm[start[data_label[i]]] = i;
      ++start[data_label[i]];
    }
    start[0] = 0;
    for(int i = 1; i < nr_class; i++) {
      start[i] = start[i - 1] + count[i - 1];
    }

    // Fill return array:
    group_ret[0] = label;
    group_ret[1] = start;
    group_ret[2] = count;
    return nr_class;
  }

  public static boolean nonzero(double v) {
    return v > 0. || v < 0.;
  }

  abstract protected Logging getLogger();
}
