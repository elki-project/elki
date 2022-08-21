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

import net.jafama.FastMath;

/**
 * Simple spherical Gaussian cluster.
 * 
 * @author Andreas Lang
 * @since 0.7.0
 */
public class TextbookSphericalGaussianModel implements EMClusterModel<NumberVector, EMModel> {
  /**
   * Mean vector.
   */
  double[] mean;

  /**
   * Variances.
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
   * Prior variance, for MAP estimation.
   */
  double priorvar;

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   */
  public TextbookSphericalGaussianModel(double weight, double[] mean) {
    this(weight, mean, 1.);
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param var Initial variance
   */
  public TextbookSphericalGaussianModel(double weight, double[] mean, double var) {
    this.weight = weight;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.logNormDet = FastMath.log(weight) - .5 * logNorm;
    this.nmea = new double[mean.length];
    this.variance = var > 0 ? var : 1e-10;
    this.priorvar = this.variance;
    this.wsum = 0.;
  }

  @Override
  public void beginEStep() {
    wsum = 0.;
    clear(mean);
    variance = 0.;
  }

  @Override
  public void updateE(NumberVector vec, double wei) {
    final int dim = mean.length;
    assert (vec.getDimensionality() == dim);
    assert (wei >= 0 && wei < Double.POSITIVE_INFINITY) : wei;
    // Compute new means and variance
    for(int i = 0; i < mean.length; i++) {
      double vi = vec.doubleValue(i);
      double vi_wei = vi * wei;
      mean[i] += vi_wei;
      variance += vi_wei * vi;
    }
    // Use new values.
    wsum += wei;
    // System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep(double weight, double prior) {
    final int dim = mean.length;
    this.weight = weight;
    double logDet = 0.;
    final double f = (wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY) ? 1. / wsum : 1.;
    for(int i = 0; i < dim; i++) {
      mean[i] *= f;
    }
    if(prior > 0) { // TODO MAP
      double nu = dim + 2; // Popular default.F
      double f2 = 1. / (wsum + prior * (nu + dim + 2));
      double newvar = 0.;
      for(int i = 0; i < dim; i++) {
        newvar += (variance - mean[i] * mean[i] * wsum + prior * priorvar) * f2;
      }
      variance = newvar;
      logDet = FastMath.log(variance); // * dim ?
    }
    else if(wsum > 0.) { // MLE
      double newvar = 0.;
      final double wf = (wsum > Double.MIN_NORMAL && wsum < Double.POSITIVE_INFINITY) ? 1. / (wsum * dim) : 1. / dim;
      for(int i = 0; i < dim; i++) {
        newvar += (variance * wf - (mean[i] * mean[i]));
      }
      variance = newvar / dim;
      logDet = FastMath.log(variance) * dim;
    } // Else degenerate
    logNormDet = FastMath.log(weight) - .5 * (logNorm + logDet);
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
}
