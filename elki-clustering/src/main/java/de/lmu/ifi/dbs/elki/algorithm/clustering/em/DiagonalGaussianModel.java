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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.math.MathUtil;

import net.jafama.FastMath;

/**
 * Simpler model for a single Gaussian cluster, without covariances.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DiagonalGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Constant to avoid singular matrixes.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

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
   * Weight aggregation sum
   */
  double weight, wsum;

  /**
   * For the MAP version only, a prior diagonal
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
   * @param variances Initial variances.
   */
  public DiagonalGaussianModel(double weight, double[] mean, double[] variances) {
    this.weight = weight;
    final int dim = mean.length;
    this.mean = mean;
    this.logNorm = MathUtil.LOGTWOPI * mean.length;
    this.logNormDet = FastMath.log(weight) - .5 * logNorm;
    this.nmea = new double[dim];
    if(variances == null) {
      this.variances = new double[dim];
      Arrays.fill(variances, 1.);
    }
    else {
      this.variances = variances;
      this.priordiag = copy(variances);
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
    assert (vec.getDimensionality() == mean.length);
    final double nwsum = wsum + wei;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = vec.doubleValue(i) - mean[i];
      final double rval = delta * wei / nwsum;
      nmea[i] = mean[i] + rval;
    }
    // Update variances
    for(int i = 0; i < mean.length; i++) {
      // We DO want to use the new mean once and the old mean once!
      // It does not matter which one is which.
      double vi = vec.doubleValue(i);
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
    // FIXME: support prior.
    double logDet = 0;
    if(prior > 0 && priordiag != null) {
      // MAP
      double nu = dim + 2; // Popular default.
      double f2 = 1. / (wsum + prior * (nu + dim + 2));
      for(int i = 0; i < dim; i++) {
        logDet += FastMath.log(variances[i] = (variances[i] + prior * priordiag[i]) * f2);
      }
    }
    else if(wsum > 0.) { // MLE
      final double s = 1. / wsum;
      for(int i = 0; i < dim; i++) {
        double v = variances[i];
        logDet += FastMath.log(variances[i] = v > 0 ? v * s : SINGULARITY_CHEAT);
      }
    } // else degenerate
    logNormDet = FastMath.log(weight) - .5 * (logNorm + logDet);
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(NumberVector vec) {
    double agg = 0.;
    for(int i = 0; i < variances.length; i++) {
      double diff = vec.doubleValue(i) - mean[i];
      agg += diff / variances[i] * diff;
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
}
