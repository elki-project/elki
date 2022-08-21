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

import static elki.math.linearalgebra.VMath.identity;
import static elki.math.linearalgebra.VMath.timesEquals;

import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.index.tree.betula.features.ClusterFeature;
import elki.math.MathUtil;

import net.jafama.FastMath;

/**
 * Simple spherical Gaussian cluster (scaled identity matrixes).
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SphericalGaussianModel implements BetulaClusterModel {
  /**
   * Constant to avoid zero values.
   */
  private static final double SINGULARITY_CHEAT = 1E-10;

  /**
   * Mean vector.
   */
  double[] mean;

  /**
   * Single variances for all dimensions.
   */
  double variance;

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
   * Prior variance.
   */
  double priorvar;

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   */
  public SphericalGaussianModel(double weight, double[] mean) {
    this(weight, mean, 1.);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param var Initial variance
   */
  public SphericalGaussianModel(double weight, double[] mean, double var) {
    this.weight = weight;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.logNormDet = FastMath.log(weight) - .5 * logNorm;
    this.nmea = new double[mean.length];
    this.variance = var > 0 ? var : SINGULARITY_CHEAT;
    this.priorvar = this.variance;
    this.wsum = 0.;
  }

  @Override
  public void beginEStep() {
    wsum = 0.;
    variance = 0.;
  }

  @Override
  public void updateE(NumberVector vec, double wei) {
    assert vec.getDimensionality() == mean.length;
    assert wei >= 0 && wei < Double.POSITIVE_INFINITY : wei;
    if(wei < Double.MIN_NORMAL) {
      return;
    }
    final double nwsum = wsum + wei;
    final double f = wei / nwsum; // Do division only once
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      nmea[i] = mean[i] + (vec.doubleValue(i) - mean[i]) * f;
    }
    // Update variances
    for(int i = 0; i < mean.length; i++) {
      final double vi = vec.doubleValue(i);
      // We DO want to use the new mean once and the old mean once!
      // It does not matter which one is which.
      variance += (vi - nmea[i]) * (vi - mean[i]) * wei;
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    final int dim = mean.length;
    this.weight = weight;
    if(prior > 0 && priorvar > 0) { // MAP
      double nu = dim + 2.; // Popular default.
      variance = variance / dim + prior * priorvar;
      variance /= (wsum + prior * (nu + dim + 2));
    }
    else if(wsum > 0.) { // MLE
      variance /= dim * wsum; // variance sum -> average variance
    } // else: variance must be 0
    // Note: for dim dimenions, we have dim times the variance
    double logDet = dim * FastMath.log(MathUtil.max(variance, SINGULARITY_CHEAT));
    logNormDet = FastMath.log(weight) - .5 * (logNorm + logDet);
    if(prior > 0 && priorvar == 0) {
      priorvar = variance;
    }
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(double[] vec) {
    double agg = 0.;
    for(int i = 0; i < vec.length; i++) {
      double diff = vec[i] - mean[i];
      agg += diff / variance * diff;
    }
    return agg;
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(NumberVector vec) {
    double agg = 0.;
    for(int i = 0; i < mean.length; i++) {
      double diff = vec.doubleValue(i) - mean[i];
      agg += diff / variance * diff;
    }
    return agg;
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
    return new EMModel(mean, timesEquals(identity(nmea.length, nmea.length), variance));
  }

  @Override
  public double estimateLogDensity(ClusterFeature cf) {
    final int dim = mean.length;
    final double v = cf.variance() / dim + variance;
    double agg = 0.;
    for(int i = 0; i < dim; i++) {
      final double diff = cf.centroid(i) - mean[i];
      agg += diff / v * diff;
    }
    return -.5 * (agg + logNorm + FastMath.log(v) * dim);
  }

  @Override
  public void updateE(ClusterFeature cf, double wei) {
    assert cf.getDimensionality() == mean.length;
    final double nwsum = wsum + wei;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      nmea[i] = mean[i] + (cf.centroid(i) - mean[i]) * wei / nwsum;
    }
    // Update variances
    final double ovar = variance / mean.length;
    variance = 0;
    for(int i = 0; i < mean.length; i++) {
      final double vi = cf.centroid(i);
      // variance contains SSE
      variance += ovar + wei * cf.variance(i) + wei * (vi - nmea[i]) * (vi - mean[i]);
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }
}
