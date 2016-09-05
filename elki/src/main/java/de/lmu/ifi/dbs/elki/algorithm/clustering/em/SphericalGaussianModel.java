package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.identity;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Simple spherical Gaussian cluster.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SphericalGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(SphericalGaussianModel.class);

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
  public SphericalGaussianModel(double weight, double[] mean) {
    this(weight, mean, MathUtil.powi(MathUtil.TWOPI, mean.length));
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param norm Normalization factor.
   */
  public SphericalGaussianModel(double weight, double[] mean, double norm) {
    this.weight = weight;
    this.mean = mean;
    this.norm = norm;
    this.normDistrFactor = 1. / Math.sqrt(norm); // assume det=1
    this.nmea = new double[mean.length];
    this.variance = 1.;
    this.wsum = 0.;
  }

  @Override
  public void beginEStep() {
    wsum = 0.;
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
      variance += (vi - nmea[i]) * (vi - mean[i]) * wei;
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep() {
    if(wsum > 0.) {
      variance = variance / (wsum * mean.length);
      normDistrFactor = 1. / Math.sqrt(norm * variance);
    }
    else {
      // Degenerate
      normDistrFactor = 1. / Math.sqrt(norm);
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
  public double estimateDensity(NumberVector vec) {
    double power = mahalanobisDistance(vec) * .5;
    double prob = normDistrFactor * Math.exp(-power);
    if(!(prob >= 0.)) {
      LOG.warning("Invalid probability: " + prob + " power: " + power + " factor: " + normDistrFactor);
      prob = 0.;
    }
    return prob * weight;
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