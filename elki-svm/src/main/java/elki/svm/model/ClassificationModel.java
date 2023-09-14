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
package elki.svm.model;

import elki.svm.data.DataSet;

/**
 * A SVM classification model.
 */
public class ClassificationModel extends Model {
  /** Labels of support vectors */
  public int[] label;

  /** Number of support vectors per class */
  public int[] nSV;

  // nSV[0] + nSV[1] + ... + nSV[k-1] = l

  /**
   * Predict for a single data point.
   *
   * @param x Data set
   * @param xi Index of data point to predict for
   * @param dec_values Decision values output
   * @return Predicted class
   */
  public int predict(DataSet x, int xi, double[] dec_values) {
    double[] kvalue = new double[l];
    for(int i = 0; i < l; i++) {
      kvalue[i] = x.similarity(xi, sv_indices[i]);
    }

    int[] start = new int[nr_class];
    for(int i = 1; i < nr_class; i++) {
      start[i] = start[i - 1] + nSV[i - 1];
    }

    int[] vote = new int[nr_class];

    int p = 0;
    for(int i = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, p++) {
        double sum = 0;
        int si = start[i], sj = start[j];
        int ci = nSV[i], cj = nSV[j];

        double[] coef1 = sv_coef[j - 1], coef2 = sv_coef[i];
        for(int k = 0; k < ci; k++) {
          sum += coef1[si + k] * kvalue[si + k];
        }
        for(int k = 0; k < cj; k++) {
          sum += coef2[sj + k] * kvalue[sj + k];
        }
        sum -= rho[p];
        dec_values[p] = sum;

        vote[(sum > 0) ? i : j]++;
      }
    }

    int vote_max_idx = 0;
    for(int i = 1; i < nr_class; i++) {
      if(vote[i] > vote[vote_max_idx]) {
        vote_max_idx = i;
      }
    }
    return label[vote_max_idx];
  }

  /**
   * Predict for a single data point.
   *
   * @param x Data set
   * @param xi Index of data point to predict for
   * @return Predicted class
   */
  public int predict(DataSet x, int xi) {
    double[] kvalue = new double[l];
    for(int i = 0; i < l; i++) {
      kvalue[i] = x.similarity(xi, sv_indices[i]);
    }

    int[] start = new int[nr_class];
    for(int i = 1; i < nr_class; i++) {
      start[i] = start[i - 1] + nSV[i - 1];
    }

    int[] vote = new int[nr_class];

    int p = 0;
    for(int i = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, p++) {
        double sum = 0;
        int si = start[i], sj = start[j];
        int ci = nSV[i], cj = nSV[j];

        double[] coef1 = sv_coef[j - 1], coef2 = sv_coef[i];
        for(int k = 0; k < ci; k++) {
          sum += coef1[si + k] * kvalue[si + k];
        }
        for(int k = 0; k < cj; k++) {
          sum += coef2[sj + k] * kvalue[sj + k];
        }
        sum -= rho[p];

        vote[(sum > 0) ? i : j]++;
      }
    }

    int vote_max_idx = 0;
    for(int i = 1; i < nr_class; i++) {
      if(vote[i] > vote[vote_max_idx]) {
        vote_max_idx = i;
      }
    }

    return label[vote_max_idx];
  }
}
