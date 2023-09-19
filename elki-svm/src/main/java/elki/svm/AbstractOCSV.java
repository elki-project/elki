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

import elki.svm.data.DataSet;
import elki.svm.model.OneClassModel;
import elki.svm.solver.Solver.SolutionInfo;

/**
 * Abstract One-Class Support Vector Machine
 */
public abstract class AbstractOCSV extends AbstractSingleSVM {
  /**
   * Estimate probabilities with binning method
   */
  boolean probability = false;

  /**
   * Constructor.
   * 
   * @param tol Optimizer tolerance
   * @param shrinking Use shrinking
   * @param cache_size Cache size
   * @param probability Estimate probabilities
   */
  public AbstractOCSV(double tol, boolean shrinking, double cache_size, boolean probability) {
    super(tol, shrinking, cache_size);
    this.probability = probability;
  }

  /**
   * Get parameters for simple one-class SVM probability estimates
   * 
   * @param model One-class model
   * @param x Data set
   * @param nr_marks Number of bins
   * @return Bin thresholds
   */
  protected double[] probability_bins(OneClassModel model, DataSet x, int nr_marks) {
    final int l = x.size();
    double[] dec_value = new double[1];
    double[] dec_values = new double[l];
    double[] pred_results = new double[l];
    // Predict for the training data:
    for(int i = 0; i < l; i++) {
      pred_results[i] = model.predict(x, i, dec_value);
      dec_values[i] = dec_value[0];
    }
    Arrays.sort(dec_values);

    // Count the number of negative/positive entries
    int neg_counter = 0;
    for(int i = 0; i < l; i++) {
      if(dec_values[i] >= 0) {
        neg_counter = i;
        break;
      }
    }

    final int half_marks = nr_marks >> 1;
    final int pos_counter = l - neg_counter;
    if(neg_counter < half_marks || pos_counter < half_marks) {
      getLogger().debug("WARNING: number of positive or negative decision values <" + half_marks + "; too few to do a probability estimation.");
      return null;
    }
    // Binning by density
    double[] tmp_marks = new double[nr_marks + 1];
    int mid = nr_marks >> 1;
    for(int i = 0; i < mid; i++) {
      tmp_marks[i] = dec_values[i * neg_counter / mid];
    }
    tmp_marks[mid] = 0;
    for(int i = mid + 1; i < nr_marks + 1; i++) {
      tmp_marks[i] = dec_values[neg_counter - 1 + (i - mid) * pos_counter / mid];
    }

    double[] prob_density_marks = new double[nr_marks];
    for(int i = 0; i < nr_marks; i++) {
      prob_density_marks[i] = (tmp_marks[i] + tmp_marks[i + 1]) * 0.5;
    }
    return prob_density_marks;
  }

  /**
   * Binning method from the oneclass_prob paper by Que and Lin to predict the
   * probability as a normal instance (i.e., not an outlier)
   * 
   * @param model Model
   * @param bins Binning thresholds
   * @param dec_value Decision value
   * @return Probability
   */
  public static double predict_probability(OneClassModel model, double[] bins, double dec_value) {
    double prob_estimate = 0.0;
    final int nr_marks = bins.length;

    if(dec_value < bins[0]) {
      return 0.0001;
    }
    else if(dec_value > bins[nr_marks - 1]) {
      return 0.999;
    }
    for(int i = 0; i < nr_marks; i++) {
      // TODO: perform some interpolation to get more differentiation and retain order?
      if(dec_value < bins[i]) {
        prob_estimate = (double) i / nr_marks;
        break;
      }
    }
    return prob_estimate;
  }

  /**
   * Train on a data set.
   * 
   * @param x Data set
   * @return Regression model
   */
  public OneClassModel train(DataSet x) {
    SolutionInfo si = train_one(x);
    OneClassModel model = new OneClassModel();
    model.nr_class = 2;
    model.sv_coef = new double[1][];
    model.rho = new double[] { si.rho };
    model.r_square = si.r_square;

    int nSV = 0;
    for(int i = 0; i < x.size(); i++) {
      if(nonzero(si.alpha[i])) {
        ++nSV;
      }
    }
    double[] coef = model.sv_coef[0] = new double[nSV];
    model.sv_indices = new int[nSV];
    for(int i = 0, j = 0; i < x.size(); i++) {
      if(nonzero(si.alpha[i])) {
        coef[j] = si.alpha[i];
        model.sv_indices[j] = i;
        ++j;
      }
    }
    return model;
  }
}
