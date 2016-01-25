package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Simpler model for a single Gaussian cluster, without covariances.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DiagonalGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(DiagonalGaussianModel.class);

  /**
   * Mean vector.
   */
  Vector mean;

  /**
   * Per-dimension variances.
   */
  double[] variances;

  /**
   * Temporary storage, to avoid reallocations.
   */
  double[] nmea, mref;

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
  public DiagonalGaussianModel(double weight, Vector mean) {
    this(weight, mean, MathUtil.powi(MathUtil.TWOPI, mean.getDimensionality()));
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param norm Normalization factor.
   */
  public DiagonalGaussianModel(double weight, Vector mean, double norm) {
    this.weight = weight;
    final int dim = mean.getDimensionality();
    this.mean = mean;
    this.norm = norm;
    this.normDistrFactor = 1. / Math.sqrt(norm); // assume det=1
    this.mref = mean.getArrayRef();
    this.nmea = new double[dim];
    this.variances = new double[dim];
    Arrays.fill(variances, 1.);
    this.wsum = 0.;
  }

  @Override
  public void beginEStep() {
    wsum = 0.;
  }

  @Override
  public void updateE(NumberVector vec, double wei) {
    assert (vec.getDimensionality() == mref.length);
    final double nwsum = wsum + wei;
    // Compute new means
    for(int i = 0; i < mref.length; i++) {
      final double delta = vec.doubleValue(i) - mref[i];
      final double rval = delta * wei / nwsum;
      nmea[i] = mref[i] + rval;
    }
    // Update variances
    for(int i = 0; i < mref.length; i++) {
      // We DO want to use the new mean once and the old mean once!
      // It does not matter which one is which.
      variances[i] += (vec.doubleValue(i) - nmea[i]) * (vec.doubleValue(i) - mref[i]) * wei;
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mref, 0, nmea.length);
  }

  @Override
  public void finalizeEStep() {
    if(wsum > 0.) {
      final double s = 1. / wsum;
      double det = 1.;
      for(int i = 0; i < variances.length; i++) {
        double v = variances[i];
        v = v > 0 ? v * s : Matrix.SINGULARITY_CHEAT;
        variances[i] = v;
        det *= v;
      }
      normDistrFactor = 1. / Math.sqrt(norm * det);
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
  public double mahalanobisDistance(Vector vec) {
    double[] difference = vec.minus(mean).getArrayRef();
    double agg = 0.;
    for(int i = 0; i < variances.length; i++) {
      agg += difference[i] / variances[i] * difference[i];
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
    double[] difference = vec.getColumnVector().minusEquals(mean).getArrayRef();
    double agg = 0.;
    for(int i = 0; i < variances.length; i++) {
      agg += difference[i] / variances[i] * difference[i];
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
    return new EMModel(mean, Matrix.diagonal(variances));
  }
}