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
import elki.index.tree.betula.features.ClusterFeature;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.math.linearalgebra.CholeskyDecomposition;
import net.jafama.FastMath;

/**
 * Model for a single multivariate Gaussian cluster with arbitrary rotation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MultivariateGaussianModel implements BetulaClusterModel {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MultivariateGaussianModel.class);

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
  double[] nmea;

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
  public MultivariateGaussianModel(double weight, double[] mean) {
    this(weight, mean, null);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param covariance Initial covariance matrix
   */
  public MultivariateGaussianModel(double weight, double[] mean, double[][] covariance) {
    this.weight = weight;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.nmea = new double[mean.length];
    this.covariance = covariance != null ? copy(covariance) : identity(mean.length, mean.length);
    this.priormatrix = covariance != null ? covariance : null;
    this.wsum = 0.;
    this.chol = updateCholesky(this.covariance, null);
    this.logNormDet = FastMath.log(weight) - .5 * logNorm - getHalfLogDeterminant(this.chol);
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
    final double nwsum = wsum + wei;
    final double f = wei / nwsum; // Do division only once
    // Compute new means
    for(int i = 0; i < dim; i++) {
      nmea[i] = mean[i] + (vec.doubleValue(i) - mean[i]) * f;
    }
    // Update covariance matrix
    for(int i = 0; i < dim; i++) {
      double vi = vec.doubleValue(i), delta_i = vi - nmea[i];
      double[] cov_i = covariance[i];
      for(int j = 0; j < i; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        cov_i[j] += delta_i * (vec.doubleValue(j) - mean[j]) * wei;
      }
      // Element on diagonal
      cov_i[i] += delta_i * (vi - mean[i]) * wei;
      // Other half is NOT updated here, but in finalizeEStep!
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    final int dim = covariance.length;
    this.weight = weight;
    double f = wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY ? 1. / wsum : 1.;
    if(prior > 0 && priormatrix != null) { // MAP
      final double nu = dim + 2.; // Popular default.
      final double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        final double[] row_i = covariance[i], pri_i = priormatrix[i];
        for(int j = 0; j < i; j++) { // Restore symmetry & scale
          covariance[j][i] = row_i[j] = (row_i[j] + prior * pri_i[j]) * f2;
        }
        row_i[i] = (row_i[i] + prior * pri_i[i]) * f2; // Diagonal
      }
    }
    else { // MLE
      for(int i = 0; i < dim; i++) {
        final double[] row_i = covariance[i];
        for(int j = 0; j < i; j++) { // Restore symmetry & scale
          covariance[j][i] = row_i[j] *= f;
        }
        row_i[i] *= f; // Diagonal
      }
    }
    this.chol = updateCholesky(covariance, null);
    this.logNormDet = FastMath.log(weight) - .5 * logNorm - getHalfLogDeterminant(this.chol);
    if(prior > 0 && priormatrix == null) {
      priormatrix = copy(covariance);
    }
  }

  /**
   * Update the cholesky decomposition.
   * 
   * @param covariance Covariance matrix
   * @param prev Previous Cholesky decomposition (reused in case of
   *        instability)
   * @return New Cholesky decomposition
   */
  protected static CholeskyDecomposition updateCholesky(double[][] covariance, CholeskyDecomposition prev) {
    CholeskyDecomposition nextchol = new CholeskyDecomposition(covariance);
    if(nextchol.isSPD()) {
      return nextchol;
    }
    // Add a small value to the diagonal, to reduce some rounding problems.
    double s = 0.;
    for(int i = 0; i < covariance.length; i++) {
      s += covariance[i][i];
    }
    s = s > 1e-100 ? (s * SINGULARITY_CHEAT / covariance.length) : 1e-50;
    for(int i = 0; i < covariance.length; i++) {
      covariance[i][i] += s;
    }
    nextchol = new CholeskyDecomposition(covariance);
    if(!nextchol.isSPD()) {
      LOG.warning("A cluster has degenerated, likely due to lack of variance in a subset of the data or too extreme magnitude differences.\n" + //
          "The algorithm will likely stop without converging, and fail to produce a good fit.");
      return prev != null ? prev : nextchol; // Prefer previous
    }
    return nextchol;
  }

  /**
   * Get 0.5 * log(det) of a cholesky decomposition.
   * 
   * @param chol Cholesky Decomposition
   * @return log determinant.
   */
  protected static double getHalfLogDeterminant(CholeskyDecomposition chol) {
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
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(double[] vec) {
    return squareSum(chol.solveLInplace(minusEquals(vec.clone(), mean)));
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * <p>
   * Note: used by P3C.
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

  @Override
  public double estimateLogDensity(ClusterFeature cf) {
    double[][] combinedCov = cf.covariance();
    double[] delta = mean.clone();
    for(int i = 0; i < mean.length; i++) {
      delta[i] -= cf.centroid(i);
    }
    for(int i = 0; i < covariance.length; i++) {
      for(int j = 0; j <= i; j++) {
        combinedCov[j][i] = combinedCov[i][j] += covariance[i][j];
      }
    }
    CholeskyDecomposition cchol = updateCholesky(combinedCov, chol);
    double clogNormDet = FastMath.log(cf.getWeight() + wsum) - 0.5 * logNorm - getHalfLogDeterminant(cchol);
    return -0.5 * squareSum(chol.solveLInplace(delta)) + clogNormDet;
  }

  @Override
  public void updateE(ClusterFeature cf, double wei) {
    assert cf.getDimensionality() == mean.length;
    assert wei >= 0 && wei < Double.POSITIVE_INFINITY : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
    final int dim = mean.length;
    final double nwsum = wsum + wei;
    final double f = wei / nwsum; // Do division only once
    final double[][] cfcov = timesEquals(cf.covariance(), wei);
    // Compute new means and covariance
    for(int i = 0; i < dim; i++) {
      final double delta = cf.centroid(i) - mean[i];
      nmea[i] = mean[i] + delta * f;
      for(int j = 0; j <= i; j++) {
        covariance[i][j] += cfcov[i][j] + wei * (delta * (cf.centroid(j) - nmea[j]));
      }
      // Other half is NOT updated here, but in finalizeEStep!
    }
    // Use new Values
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }
}
