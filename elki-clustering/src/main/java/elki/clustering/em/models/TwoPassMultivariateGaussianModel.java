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
package elki.clustering.em.models;

import static elki.math.linearalgebra.VMath.*;

import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.math.MathUtil;
import elki.math.linearalgebra.CholeskyDecomposition;

import net.jafama.FastMath;

/**
 * Model for a single Gaussian cluster, using two-passes for slightly better
 * numerics.
 * <p>
 * This is the more classic approach, but the savings in numerical precision are
 * usually negligible, since we already use a very stable and fast approach.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class TwoPassMultivariateGaussianModel implements EMClusterModel<NumberVector, EMModel> {
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
  public TwoPassMultivariateGaussianModel(double weight, double[] mean) {
    this(weight, mean, null);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param covariance initial covariance matrix
   */
  public TwoPassMultivariateGaussianModel(double weight, double[] mean, double[][] covariance) {
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
  public boolean needsTwoPass() {
    return true;
  }

  /**
   * First pass: update the mean only.
   */
  @Override
  public void firstPassE(NumberVector vec, double wei) {
    assert vec.getDimensionality() == mean.length;
    assert wei >= 0 && wei < Double.POSITIVE_INFINITY : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
    // Aggregate
    for(int i = 0; i < mean.length; i++) {
      mean[i] += vec.doubleValue(i) * wei;
    }
    wsum += wei;
  }

  /**
   * Finish computation of the mean.
   */
  @Override
  public void finalizeFirstPassE() {
    double s = 1. / wsum;
    for(int i = 0; i < mean.length; i++) {
      mean[i] *= s;
    }
  }

  /**
   * Second pass: compute the covariance matrix.
   */
  @Override
  public void updateE(NumberVector vec, double wei) {
    assert vec.getDimensionality() == mean.length;
    assert wei >= 0 && wei < Double.POSITIVE_INFINITY : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
    final int dim = mean.length;
    for(int i = 0; i < dim; i++) {
      // Center the vector:
      double vi = tmp[i] = vec.doubleValue(i) - mean[i], vi_wei = vi * wei;
      double[] cov_i = covariance[i];
      // j < i is already centered in tmp.
      for(int j = 0; j < i; j++) {
        cov_i[j] += vi_wei * tmp[j];
      }
      // Element on diagonal
      cov_i[i] += vi_wei * vi;
      // Other half is NOT updated here, but in finalizeEStep!
    }
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    final int dim = covariance.length;
    this.weight = weight;
    double f = wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY ? 1. / wsum : 1.;
    if(prior > 0 && priormatrix != null) { // MAP
      double nu = dim + 2.; // Popular default.
      double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        double[] row_i = covariance[i], pri_i = priormatrix[i];
        for(int j = 0; j < i; j++) { // Restore symmetry & scale
          covariance[j][i] = row_i[j] = (row_i[j] + prior * pri_i[j]) * f2;
        }
        row_i[i] = (row_i[i] + prior * pri_i[i]) * f2; // Diagonal
      }
    }
    else { // MLE
      for(int i = 0; i < dim; i++) {
        double[] row_i = covariance[i];
        for(int j = 0; j < i; j++) { // Restore symmetry & scale
          covariance[j][i] = row_i[j] *= f;
        }
        row_i[i] *= f; // Diagonal
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
