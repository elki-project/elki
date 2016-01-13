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
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimes;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimesTimes;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LUDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Model for a single Gaussian cluster.
 * 
 * @author Erich Schubert
 */
public class MultivariateGaussianModel implements EMClusterModel<EMModel> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(MultivariateGaussianModel.class);

  /**
   * Mean vector.
   */
  double[] mean;

  /**
   * Covariance matrix, and inverse.
   */
  Matrix covariance, invCovMatr;

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
    this(weight, mean, MathUtil.powi(MathUtil.TWOPI, mean.length));
  }

  /**
   * Constructor.
   * 
   * @param weight Cluster weight
   * @param mean Initial mean
   * @param norm Normalization factor.
   */
  public MultivariateGaussianModel(double weight, double[] mean, double norm) {
    this.weight = weight;
    final int dim = mean.length;
    this.mean = mean;
    this.norm = norm;
    this.normDistrFactor = 1. / Math.sqrt(norm); // assume det=1
    this.nmea = new double[dim];
    this.covariance = new Matrix(dim, dim);
    this.wsum = 0.;
  }

  @Override
  public void beginEStep() {
    if(covariance == null) {
      covariance = new Matrix(mean.length, mean.length);
      return;
    }
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
    // Update covariance matrix
    final double[][] elements = covariance.getArrayRef();
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double vi = vec.doubleValue(i);
        double delta = (vi - nmea[i]) * (vec.doubleValue(j) - mean[j]) * wei;
        elements[i][j] = elements[i][j] + delta;
        // Optimize via symmetry
        if(i != j) {
          elements[j][i] = elements[j][i] + delta;
        }
      }
    }
    // Use new values.
    wsum = nwsum;
    System.arraycopy(nmea, 0, mean, 0, nmea.length);
  }

  @Override
  public void finalizeEStep() {
    final int dim = mean.length;
    // TODO: improve handling of degenerated cases?
    if(wsum > Double.MIN_NORMAL) {
      covariance.timesEquals(1. / wsum);
    }
    LUDecomposition lu = new LUDecomposition(covariance);
    double det = lu.det();
    if(!(det > 0.)) {
      // Add a small value to the diagonal
      covariance.plusDiagonalEquals(Matrix.SINGULARITY_CHEAT);
      lu = new LUDecomposition(covariance); // Should no longer be zero now.
      det = lu.det();
      if(!(det > 0.)) {
        LOG.warning("Singularity cheat did not resolve zero determinant.");
        // assert (det > 0) : "Singularity cheat did not resolve zero
        // determinant.";
        det = 1.;
      }
    }
    normDistrFactor = 1. / Math.sqrt(norm * det);
    invCovMatr = lu.solve(Matrix.identity(dim, dim));
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(double[] vec) {
    if(invCovMatr != null) {
      return VMath.mahalanobisDistance(invCovMatr, vec, mean);
    }
    double sqsum = 0.;
    for (int i = 0; i < vec.length; i++) {
      double d = vec[i] - mean[i];
      sqsum += d * d;
    }
    return sqsum;
  }

  /**
   * Compute the Mahalanobis distance from the centroid for a given vector.
   * 
   * @param vec Vector
   * @return Mahalanobis distance
   */
  public double mahalanobisDistance(NumberVector vec) {
    double[] difference = minusEquals(vec.toArray(), mean);
    return (invCovMatr != null) ? transposeTimesTimes(difference, invCovMatr, difference) : transposeTimes(difference, difference);
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
    return new EMModel(mean, covariance);
  }
}