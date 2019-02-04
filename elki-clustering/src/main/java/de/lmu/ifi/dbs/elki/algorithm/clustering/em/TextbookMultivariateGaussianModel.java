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
package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CholeskyDecomposition;

import net.jafama.FastMath;

/**
 * Numerically problematic implementation of the GMM model, using the textbook
 * algorithm. There is no reason to use this in practice, it is only useful to
 * study the reliability of the textbook approach.
 * <p>
 * "Textbook" refers to the E[XY]-E[X]E[Y] equation for covariance, that is
 * numerically not reliable with floating point math, but popular in textbooks.
 * <p>
 * Again, do not use this. Always prefer {@link MultivariateGaussianModel}.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class TextbookMultivariateGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(TextbookMultivariateGaussianModel.class);

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
  public TextbookMultivariateGaussianModel(double weight, double[] mean) {
    this(weight, mean, null);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param covariance initial covariance matrix.
   */
  public TextbookMultivariateGaussianModel(double weight, double[] mean, double[][] covariance) {
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
  public void updateE(NumberVector vec, double wei) {
    final int dim = mean.length;
    assert (vec.getDimensionality() == dim);
    assert (wei >= 0 && wei < Double.POSITIVE_INFINITY) : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
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
    this.weight = weight;
    this.prior = prior;

    // Restore symmetry, and apply weight:
    final int dim = covariance.length;
    final double f = (wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY) ? 1. / wsum : 1.;
    assert (f > 0) : wsum;
    // Scale sum -> mean:
    for(int i = 0; i < dim; i++) {
      mean[i] *= f;
    }
    // Generate final covariance matrixes
    if(prior > 0 && priormatrix != null) {
      // MAP
      double nu = dim + 2; // Popular default.
      double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        double[] row_i = covariance[i], pri_i = priormatrix[i];
        double fi = mean[i] * wsum;
        for(int j = 0; j < i; j++) {
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
        for(int j = 0; j < i; j++) {
          covariance[j][i] = covariance_i[j] = (covariance_i[j] * f) - mean_i * mean[j];
        }
        covariance_i[i] = (covariance_i[i] * f) - mean_i * mean_i;
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
