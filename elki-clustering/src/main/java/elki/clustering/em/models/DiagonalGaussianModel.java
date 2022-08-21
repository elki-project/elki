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

import java.util.Arrays;

import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.index.tree.betula.features.ClusterFeature;
import elki.math.MathUtil;
import net.jafama.FastMath;

/**
 * Simpler model for a single Gaussian cluster, without covariances.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DiagonalGaussianModel implements BetulaClusterModel {
  /**
   * Constant to avoid zero values.
   */
  private static final double SINGULARITY_CHEAT = 1E-10;

  /**
   * Mean vector.
   */
  double[] mean;

  /**
   * Per-dimension variances.
   */
  double[] variances;

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
   * Diagonal prior variances.
   */
  double[] priordiag;

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   */
  public DiagonalGaussianModel(double weight, double[] mean) {
    this(weight, mean, null);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param vars Initial variances
   */
  public DiagonalGaussianModel(double weight, double[] mean, double[] vars) {
    this.weight = weight;
    final int dim = mean.length;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.logNormDet = FastMath.log(weight) - .5 * logNorm;
    this.nmea = new double[dim];
    if(vars == null) {
      Arrays.fill(this.variances = new double[dim], 1.);
    }
    else {
      this.variances = new double[dim];
      for(int i = 0; i < dim; i++) {
        this.variances[i] = MathUtil.max(vars[i], SINGULARITY_CHEAT);
      }
      this.priordiag = vars;
    }
    for(int i = 0; i < variances.length; i++) {
      variances[i] = Math.max(variances[i], SINGULARITY_CHEAT);
    }
    this.wsum = 0.;
  }

  @Override
  public void beginEStep() {
    wsum = 0.;
    clear(mean);
    clear(variances);
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
      // We DO want to use the new mean once and the old mean once!
      // It does not matter which one is which.
      final double vi = vec.doubleValue(i);
      variances[i] += (vi - nmea[i]) * (vi - mean[i]) * wei;
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    final int dim = variances.length;
    this.weight = weight;
    double logDet = 0.;
    if(prior > 0 && priordiag != null) { // MAP
      final double nu = dim + 2.; // Popular default.
      final double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        final double v = variances[i] + prior * priordiag[i];
        logDet += FastMath.log(variances[i] = v > 0 ? v * f2 : SINGULARITY_CHEAT);
      }
    }
    else { // MLE
      final double f = wsum > 0 ? 1. / wsum : 1;
      for(int i = 0; i < dim; i++) {
        final double v = variances[i];
        logDet += FastMath.log(variances[i] = v > 0 ? v * f : SINGULARITY_CHEAT);
      }
    }
    logNormDet = FastMath.log(weight) - .5 * (logNorm + logDet);
    if(prior > 0 && priordiag == null) {
      priordiag = copy(variances);
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
    for(int i = 0; i < mean.length; i++) {
      final double diff = vec[i] - mean[i], v = variances[i];
      agg += diff / v * diff;
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
      final double diff = vec.doubleValue(i) - mean[i], v = variances[i];
      agg += diff / v * diff;
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
    return new EMModel(mean, diagonal(variances));
  }

  @Override
  public double estimateLogDensity(ClusterFeature cf) {
    double agg = logNorm;
    for(int i = 0; i < mean.length; i++) {
      final double diff = cf.centroid(i) - mean[i];
      agg += diff / (variances[i] + cf.variance(i)) * diff;
    }
    for(int i = 0; i < mean.length; i++) {
      agg += FastMath.log(variances[i] + cf.variance(i));
    }
    return -.5 * agg;
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
    for(int i = 0; i < mean.length; i++) {
      final double vi = cf.centroid(i);
      // variances contains SSE
      variances[i] += wei * cf.variance(i) + (vi - nmea[i]) * (vi - mean[i]) * wei;
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }
}
