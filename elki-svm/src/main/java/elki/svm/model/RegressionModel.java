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
 * Support vector regression model
 */
public class RegressionModel extends Model {
  /**
   * Predict for a single data point.
   * 
   * @param x Data set
   * @param xi Point offset
   * @param dec_values Decision values output
   * @return Prediction score
   */
  public double predict(DataSet x, int xi, double[] dec_values) {
    double[] sv_coef = this.sv_coef[0];
    double sum = -rho[0];
    for(int i = 0; i < l; i++) {
      sum += sv_coef[i] * x.similarity(xi, sv_indices[i]);
    }
    dec_values[0] = sum;
    return sum;
  }

  /**
   * Predict for a single data point.
   * 
   * @param x Data set
   * @param xi Point offset
   * @return Prediction score
   */
  public double predict(DataSet x, int xi) {
    double[] sv_coef = this.sv_coef[0];
    double sum = -rho[0];
    for(int i = 0; i < l; i++) {
      sum += sv_coef[i] * x.similarity(xi, sv_indices[i]);
    }
    return sum;
  }
}
