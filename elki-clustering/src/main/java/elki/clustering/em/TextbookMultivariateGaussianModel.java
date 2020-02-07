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
package elki.clustering.em;

import static elki.math.linearalgebra.VMath.*;

import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.math.MathUtil;
import elki.math.linearalgebra.CholeskyDecomposition;

import net.jafama.FastMath;

/**
 * Numerically problematic implementation of the GMM model, using the textbook
 * algorithm. There is <b>no reason to use this in practice</b>, it is only
 * useful to study the reliability of the textbook approach.
 * <p>
 * "Textbook" refers to the E[XY]-E[X]E[Y] equation for covariance, that is
 * numerically not reliable with floating point math, but popular in textbooks.
 * <p>
 * Again, do not use this. Always prefer {@link MultivariateGaussianModel}.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class TextbookMultivariateGaussianModel implements EMClusterModel<NumberVector, EMModel> {
  /**
   * Mean vector.
   */
  double[] mean;

  /**
   * Covariance matrix.
   */
  double[][] covariance;

  /**
   * Decomposition of covariance matrix.
   */
  CholeskyDecomposition chol;

  /**
   * Temporary storage, to avoid reallocations.
   */
  double[] tmp;

  /**
   * Normalization factor.
   */
  double logNorm, logNormDet;

  /**
   * Weight aggregation sum.
   */
  double weight, wsum;

  /**
   * Matrix for prior conditioning.
   */
  double[][] priormatrix;

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   */
  public TextbookMultivariateGaussianModel(double weight, double[] mean) {
    this(weight, mean, null);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param covariance initial covariance matrix
   */
  public TextbookMultivariateGaussianModel(double weight, double[] mean, double[][] covariance) {
    this.weight = weight;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.tmp = new double[mean.length];
    this.covariance = covariance != null ? copy(covariance) : identity(mean.length, mean.length);
    this.priormatrix = covariance != null ? covariance : null;
    this.wsum = 0.;
    this.chol = MultivariateGaussianModel.updateCholesky(this.covariance, null);
    this.logNormDet = FastMath.log(weight) - .5 * logNorm - MultivariateGaussianModel.getHalfLogDeterminant(this.chol);
  }

  @Override
  public void beginEStep() {
    wsum = 0.;
    clear(mean);
    clear(covariance);
  }

  @Override
  public void updateE(NumberVector vec, double wei) {
    assert vec.getDimensionality() == mean.length;
    assert wei >= 0 && wei < Double.POSITIVE_INFINITY : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
    final int dim = mean.length;
    // Naive aggregates:
    for(int i = 0; i < dim; i++) {
      double vi = tmp[i] = vec.doubleValue(i), vi_wei = vi * wei;
      mean[i] += vi_wei;
      double[] cov_i = covariance[i];
      for(int j = 0; j < i; j++) {
        cov_i[j] += vi_wei * tmp[j];
      }
      // Element on diagonal
      cov_i[i] += vi_wei * vi;
      // Other half is NOT updated here, but in finalizeEStep!
    }
    wsum += wei;
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    final int dim = covariance.length;
    this.weight = weight;
    double f = wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY ? 1. / wsum : 1.;
    // Scale sum -> mean:
    for(int i = 0; i < dim; i++) {
      mean[i] *= f;
    }
    // Generate final covariance matrixes
    if(prior > 0 && priormatrix != null) { // MAP
      double nu = dim + 2.; // Popular default.
      double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        double[] row_i = covariance[i], pri_i = priormatrix[i];
        double fi = mean[i] * wsum;
        for(int j = 0; j < i; j++) { // Restore symmetry & scale
          covariance[j][i] = row_i[j] = (row_i[j] - fi * mean[j] + prior * pri_i[j]) * f2;
        }
        // Entry on diagonal:
        row_i[i] = (row_i[i] - fi * mean[i] + prior * pri_i[i]) * f2;
      }
    }
    else { // MLE
      for(int i = 0; i < dim; i++) {
        double[] covariance_i = covariance[i];
        double mean_i = mean[i];
        // Naive, using E[XY]-E[X]E[Y]:
        for(int j = 0; j < i; j++) { // Restore symmetry & scale
          covariance[j][i] = covariance_i[j] = covariance_i[j] * f - mean_i * mean[j];
        }
        covariance_i[i] = covariance_i[i] * f - mean_i * mean_i;
      }
    }
    this.chol = MultivariateGaussianModel.updateCholesky(covariance, null);
    this.logNormDet = FastMath.log(weight) - .5 * logNorm - MultivariateGaussianModel.getHalfLogDeterminant(this.chol);
    if(prior > 0 && priormatrix == null) {
      priormatrix = copy(covariance);
    }
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(NumberVector vec) {
    return squareSum(chol.solveLInplace(minusEquals(vec.toArray(), mean)));
  }

  @Override
  public double estimateLogDensity(NumberVector vec) {
    return -.5 * mahalanobisDistance(vec) + logNormDet;
  }

  @Override
  public double getWeight() {
    return weight;
  }

  @Override
  public void setWeight(double weight) {
    this.weight = weight;
  }

  @Override
  public EMModel finalizeCluster() {
    return new EMModel(mean, covariance);
  }
}
