/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.clear;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.identity;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LUDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

import net.jafama.FastMath;

/**
 * Model for a single Gaussian cluster.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MultivariateGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(MultivariateGaussianModel.class);

  /**
   * Small value to add to the diagonal to avoid singularities.
   */
  private static final double SINGULARITY_CHEAT = 1e-9;

  /**
   * Mean vector.
   */
  double[] mean;

  /**
   * Covariance matrix, and inverse.
   */
  double[][] covariance, invCovMatr;

  /**
   * Temporary storage, to avoid reallocations.
   */
  double[] nmea;

  /**
   * Normalization factor.
   */
  double norm, normDistrFactor;

  /**
   * Weight aggregation sum
   */
  double weight, wsum;

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   */
  public MultivariateGaussianModel(double weight, double[] mean) {
    this(weight, mean, MathUtil.powi(MathUtil.TWOPI, mean.length), new double[mean.length][mean.length]);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param norm Normalization factor.
   */
  public MultivariateGaussianModel(double weight, double[] mean, double norm) {
    this(weight, mean, norm, new double[mean.length][mean.length]);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param norm Normalization factor.
   * @param covariance Covariance matrix.
   */
  public MultivariateGaussianModel(double weight, double[] mean, double norm, double[][] covariance) {
    this.weight = weight;
    this.mean = mean;
    this.norm = norm;
    this.normDistrFactor = 1. / FastMath.sqrt(norm);
    this.nmea = new double[mean.length];
    if(covariance == null) {
      this.covariance = new double[mean.length][mean.length];
    }
    else {
      this.covariance = covariance;
      robustInvert();
    }
    this.wsum = 0.;
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
    assert (wei >= 0);
    final double nwsum = wsum + wei;
    // Compute new means
    for(int i = 0; i < dim; i++) {
      final double delta = vec.doubleValue(i) - mean[i];
      final double rval = delta * wei / nwsum;
      nmea[i] = mean[i] + rval;
    }
    // Update covariance matrix
    for(int i = 0; i < dim; i++) {
      double vi = vec.doubleValue(i);
      double[] cov_i = covariance[i];
      for(int j = 0; j < i; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        cov_i[j] += (vi - nmea[i]) * (vec.doubleValue(j) - mean[j]) * wei;
      }
      // Element on diagonal
      cov_i[i] += (vi - nmea[i]) * (vi - mean[i]) * wei;
      // Other half is NOT updated here, but in finalizeEStep!
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep() {
    // Restore symmetry, and apply weight:
    final int dim = covariance.length;
    final double f = (wsum > Double.MIN_NORMAL) ? 1. / wsum : 1.;
    for(int i = 0; i < dim; i++) {
      double[] row_i = covariance[i];
      for(int j = 0; j < i; j++) {
        covariance[j][i] = row_i[j] *= f;
      }
      // Entry on diagonal:
      row_i[i] *= f;
    }
    robustInvert();
  }

  /**
   * Robust computation of the inverse covariance matrix.
   */
  private void robustInvert() {
    // TODO: further improve handling of degenerated cases?
    final int dim = mean.length;
    LUDecomposition lu = new LUDecomposition(covariance);
    double det = lu.det();
    if(!(det > 0.)) {
      // Add a small value to the diagonal
      for(int i = 0; i < dim; i++) {
        covariance[i][i] += SINGULARITY_CHEAT;
      }
      lu = new LUDecomposition(covariance);
      det = lu.det();
      if(!(det > 0.)) {
        LOG.warning("Singularity cheat did not resolve zero determinant.");
        det = 1.;
      }
    }
    normDistrFactor = 1. / FastMath.sqrt(norm * det);
    invCovMatr = lu.solve(identity(dim, dim));
  }

  @Override
  public double estimateDensity(NumberVector vec) {
    double power = mahalanobisDistance(vec);
    double prob = normDistrFactor * FastMath.exp(-.5 * power);
    if(!(prob >= 0.)) {
      LOG.warning("Invalid probability: " + prob + " power: " + -.5 * power + " factor: " + normDistrFactor);
      prob = 0.;
    }
    return prob * weight;
  }

  /**
   * Compute the Mahalanobis distance of a vector.
   * 
   * Note: used from
   * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.P3C}!
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(NumberVector vec) {
    return VMath.mahalanobisDistance(invCovMatr, vec.toArray(), mean);
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
