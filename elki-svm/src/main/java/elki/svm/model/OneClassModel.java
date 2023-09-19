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

import elki.svm.AbstractOCSV;
import elki.svm.data.DataSet;

/**
 * One-class support vector model
 */
public class OneClassModel extends Model {
  /** R² for SVDD only. */
  public double r_square;

  /**
   * Thresholds for probability prediction, may be null.
   */
  public double[] prob_density_marks;

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
    for(int i = 0; i < sv_indices.length; i++) {
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
    for(int i = 0; i < sv_indices.length; i++) {
      sum += sv_coef[i] * x.similarity(xi, sv_indices[i]);
    }
    return sum;
  }

  /**
   * Predict for a single data point.
   * 
   * @param x Data set
   * @param xi Point offset
   * @param prob_estimates Probability estimates output
   * @return Prediction score
   */
  public double predict_prob(DataSet x, int xi, double[] prob_estimates) {
    assert prob_density_marks != null : "SVM was not trained with probability prediction enabled";
    double[] sv_coef = this.sv_coef[0];
    double sum = -rho[0];
    for(int i = 0; i < sv_indices.length; i++) {
      sum += sv_coef[i] * x.similarity(xi, sv_indices[i]);
    }
    if(prob_density_marks != null) {
      prob_estimates[0] = AbstractOCSV.predict_probability(this, prob_density_marks, sum);
      prob_estimates[1] = 1 - prob_estimates[0];
    }
    return sum;
  }

  /**
   * @return the number of support vectors
   */
  public int numSV() {
    return sv_indices.length;
  }
}
