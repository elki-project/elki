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
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.math.linearalgebra.CholeskyDecomposition;

import net.jafama.FastMath;

/**
 * Model for a single Gaussian cluster, using two-passes for slightly better
 * numerics.
 *
 * This is the more classic approach, but the savings in numerical precision are
 * usually negligible, since we already use a very stable and fast approach.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class TwoPassMultivariateGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(TwoPassMultivariateGaussianModel.class);

  /**
   * Constant to avoid singular matrixes.
   */
  private static final double SINGULARITY_CHEAT = 1E-10;

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
   * Weight aggregation sum
   */
  double weight, wsum;

  /**
   * MAP prior / MLE prior.
   */
  double prior = 0;

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
   * @param covariance initial covariance matrix.
   */
  public TwoPassMultivariateGaussianModel(double weight, double[] mean, double[][] covariance) {
    this.weight = weight;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.tmp = new double[mean.length];
    this.covariance = covariance != null ? covariance : identity(mean.length, mean.length);
    this.priormatrix = covariance != null ? copy(covariance) : null;
    this.wsum = 0.;
    updateCholesky();
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
    final int dim = mean.length;
    assert (vec.getDimensionality() == dim);
    assert (wei >= 0 && wei < Double.POSITIVE_INFINITY) : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
    // Aggregate
    for(int i = 0; i < dim; i++) {
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
    final int dim = mean.length;
    assert (vec.getDimensionality() == dim);
    assert (wei >= 0 && wei < Double.POSITIVE_INFINITY) : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
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
    // Should we assert that the weight sum matches the first step?
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    this.weight = weight;
    this.prior = prior;
    // Restore symmetry, and apply weight:
    final int dim = covariance.length;
    final double f = (wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY) ? 1. / wsum : 1.;
    assert (f > 0) : wsum;
    if(prior > 0 && priormatrix != null) {
      // MAP
      double nu = dim + 2; // Popular default.
      double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        double[] row_i = covariance[i], pri_i = priormatrix[i];
        for(int j = 0; j < i; j++) {
          covariance[j][i] = row_i[j] = (row_i[j] + prior * pri_i[j]) * f2;
        }
        // Entry on diagonal:
        row_i[i] = (row_i[i] + prior * pri_i[i]) * f2;
      }
    }
    else { // MLE
      for(int i = 0; i < dim; i++) {
        double[] row_i = covariance[i];
        for(int j = 0; j < i; j++) {
          covariance[j][i] = row_i[j] *= f;
        }
        // Entry on diagonal:
        row_i[i] *= f;
      }
    }
    updateCholesky();
    if(prior > 0 && priormatrix == null) {
      priormatrix = copy(covariance);
    }
  }

  /**
   * Update the cholesky decomposition.
   */
  private void updateCholesky() {
    // TODO: further improve handling of degenerated cases?
    CholeskyDecomposition chol = new CholeskyDecomposition(covariance);
    if(!chol.isSPD()) {
      // Add a small value to the diagonal, to reduce some rounding problems.
      double s = 0.;
      for(int i = 0; i < covariance.length; i++) {
        s += covariance[i][i];
      }
      s *= SINGULARITY_CHEAT / covariance.length;
      for(int i = 0; i < covariance.length; i++) {
        covariance[i][i] += s;
      }
      chol = new CholeskyDecomposition(covariance);
    }
    if(!chol.isSPD()) {
      LOG.warning("A cluster has degenerated, likely due to lack of variance in a subset of the data or too extreme magnitude differences.\n" + //
          "The algorithm will likely stop without converging, and fail to produce a good fit.");
      chol = this.chol != null ? this.chol : chol; // Prefer previous
    }
    this.chol = chol;
    logNormDet = FastMath.log(weight) - .5 * logNorm - getHalfLogDeterminant(this.chol);
  }

  /**
   * Get 0.5 * log(det) of a cholesky decomposition.
   * 
   * @param chol Cholesky Decomposition
   * @return log determinant.
   */
  private double getHalfLogDeterminant(CholeskyDecomposition chol) {
    double[][] l = chol.getL();
    double logdet = FastMath.log(l[0][0]);
    for(int i = 1; i < l.length; i++) {
      // We get half the log(det), because we did not square values here.
      logdet += FastMath.log(l[i][i]);
    }
    return logdet;
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   *
   * Note: used by P3C.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(NumberVector vec) {
    // TODO: this allocates one array.
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
