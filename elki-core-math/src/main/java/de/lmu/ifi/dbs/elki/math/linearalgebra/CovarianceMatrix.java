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
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;

/**
 * Class for computing covariance matrixes using stable mean and variance
 * computations.
 * 
 * This class encapsulates the mathematical aspects of computing this matrix.
 * 
 * See {@link de.lmu.ifi.dbs.elki.database.DatabaseUtil DatabaseUtil} for
 * easier to use APIs.
 * 
 * For use in algorithms, it is more appropriate to use
 * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.StandardCovarianceMatrixBuilder
 * StandardCovarianceMatrixBuilder} since this class can be overridden with a
 * stabilized covariance matrix builder!
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - - - NumberVector
 * @navhas - produces - Matrix
 */
public class CovarianceMatrix {
  /**
   * Error message reported when too little data (weight &lt;= 1) in matrix.
   */
  public static final String ERR_TOO_LITTLE_WEIGHT = "Too few elements (too little total weight) used to obtain a valid covariance matrix.";

  /**
   * The means.
   */
  double[] mean;

  /**
   * The covariance matrix.
   */
  double[][] elements;

  /**
   * Temporary storage, to avoid reallocations.
   */
  double[] nmea;

  /**
   * The current weight.
   */
  protected double wsum;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   */
  public CovarianceMatrix(int dim) {
    super();
    this.mean = new double[dim];
    this.nmea = new double[dim];
    this.elements = new double[dim][dim];
    this.wsum = 0.;
  }

  /**
   * Get the matrix dimensionality.
   * 
   * @return Mean length.
   */
  public int getDimensionality() {
    return mean.length;
  }

  /**
   * Add a single value with weight 1.0.
   * 
   * @param val Value
   */
  public void put(double[] val) {
    assert (val.length == mean.length);
    final double nwsum = wsum + 1.;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val[i] - mean[i];
      nmea[i] = mean[i] + delta / nwsum;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val[i] - nmea[i]) * (val[j] - mean[j]);
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

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public void put(double[] val, double weight) {
    assert (val.length == mean.length);
    final double nwsum = wsum + weight;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val[i] - mean[i];
      final double rval = delta * weight / nwsum;
      nmea[i] = mean[i] + rval;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val[i] - nmea[i]) * (val[j] - mean[j]) * weight;
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

  /**
   * Add a single value with weight 1.0.
   * 
   * @param val Value
   */
  public void put(NumberVector val) {
    assert (val.getDimensionality() == mean.length);
    final double nwsum = wsum + 1.;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val.doubleValue(i) - mean[i];
      nmea[i] = mean[i] + delta / nwsum;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val.doubleValue(i) - nmea[i]) * (val.doubleValue(j) - mean[j]);
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

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public void put(NumberVector val, double weight) {
    assert (val.getDimensionality() == mean.length);
    final double nwsum = wsum + weight;
    // Compute new means
    for(int i = 0; i < mean.length; i++) {
      final double delta = val.doubleValue(i) - mean[i];
      final double rval = delta * weight / nwsum;
      nmea[i] = mean[i] + rval;
    }
    // Update covariance matrix
    for(int i = 0; i < mean.length; i++) {
      for(int j = i; j < mean.length; j++) {
        // We DO want to use the new mean once and the old mean once!
        // It does not matter which one is which.
        double delta = (val.doubleValue(i) - nmea[i]) * (val.doubleValue(j) - mean[j]) * weight;
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

  /**
   * Get the weight sum, to test whether the covariance matrix can be
   * materialized.
   * 
   * @return Weight sum.
   */
  public double getWeight() {
    return wsum;
  }

  /**
   * Get the mean as vector.
   * 
   * @return Mean vector
   */
  public double[] getMeanVector() {
    return mean;
  }

  /**
   * Get the mean as vector.
   * 
   * @param relation Data relation
   * @param <F> vector type
   * @return Mean vector
   */
  public <F extends NumberVector> F getMeanVector(Relation<? extends F> relation) {
    return RelationUtil.getNumberVectorFactory(relation).newNumberVector(mean);
  }

  /**
   * Obtain the covariance matrix according to the sample statistics: (n-1)
   * degrees of freedom.
   * 
   * This method duplicates the matrix contents, so it does allow further
   * updates. Use {@link #destroyToSampleMatrix()} if you do not need further
   * updates.
   * 
   * @return New matrix
   */
  public double[][] makeSampleMatrix() {
    if(wsum <= 1.) {
      throw new IllegalStateException(ERR_TOO_LITTLE_WEIGHT);
    }
    return times(elements, 1. / (wsum - 1.));
  }

  /**
   * Obtain the covariance matrix according to the population statistics: n
   * degrees of freedom.
   * 
   * This method duplicates the matrix contents, so it does allow further
   * updates. Use {@link #destroyToPopulationMatrix()} if you do not need further
   * updates.
   * 
   * @return New matrix
   */
  public double[][] makePopulationMatrix() {
    if(wsum <= 0.) {
      throw new IllegalStateException(ERR_TOO_LITTLE_WEIGHT);
    }
    return times(elements, 1. / wsum);
  }

  /**
   * Obtain the covariance matrix according to the sample statistics: (n-1)
   * degrees of freedom.
   * 
   * This method doesn't require matrix duplication, but will not allow further
   * updates, the object should be discarded. Use {@link #makeSampleMatrix()} if
   * you want to perform further updates.
   * 
   * @return New matrix
   */
  public double[][] destroyToSampleMatrix() {
    if(wsum <= 1.) {
      throw new IllegalStateException(ERR_TOO_LITTLE_WEIGHT);
    }
    return timesEquals(elements, 1. / (wsum - 1.));
  }

  /**
   * Obtain the covariance matrix according to the population statistics: n
   * degrees of freedom.
   * 
   * This method doesn't require matrix duplication, but will not allow further
   * updates, the object should be discarded. Use {@link #makePopulationMatrix()} if
   * you want to perform further updates.
   * 
   * @return New matrix
   */
  public double[][] destroyToPopulationMatrix() {
    if(wsum <= 0.) {
      throw new IllegalStateException(ERR_TOO_LITTLE_WEIGHT);
    }
    return timesEquals(elements, 1. / wsum);
  }

  /**
   * Reset the covariance matrix.
   * 
   * This function <em>may</em> be called after a "destroy".
   */
  public void reset() {
    Arrays.fill(mean, 0.);
    Arrays.fill(nmea, 0.);
    if(elements != null) {
      for(int i = 0; i < elements.length; i++) {
        Arrays.fill(elements[i], 0.);
      }
    }
    else {
      elements = new double[mean.length][mean.length];
    }
    wsum = 0.;
  }

  /**
   * Static Constructor from a full relation.
   * 
   * @param relation Relation to use.
   * @return Covariance matrix
   */
  public static CovarianceMatrix make(Relation<? extends NumberVector> relation) {
    int dim = RelationUtil.dimensionality(relation);
    CovarianceMatrix c = new CovarianceMatrix(dim);
    double[] mean = c.mean;
    int count = 0;
    // Compute mean first:
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      for(int i = 0; i < dim; i++) {
        mean[i] += vec.doubleValue(i);
      }
      count++;
    }
    if(count == 0) {
      return c;
    }
    // Normalize mean
    for(int i = 0; i < dim; i++) {
      mean[i] /= count;
    }
    // Compute covariances second
    // Two-pass approach is numerically okay and fast, when possible.
    double[] tmp = c.nmea; // Scratch space
    double[][] elems = c.elements;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      for(int i = 0; i < dim; i++) {
        tmp[i] = vec.doubleValue(i) - mean[i];
      }
      for(int i = 0; i < dim; i++) {
        for(int j = i; j < dim; j++) {
          elems[i][j] += tmp[i] * tmp[j];
        }
      }
    }
    // Restore symmetry.
    for(int i = 0; i < dim; i++) {
      for(int j = i + 1; j < dim; j++) {
        elems[j][i] = elems[i][j];
      }
    }
    c.wsum = count;
    return c;
  }

  /**
   * Static Constructor from a full relation.
   * 
   * @param relation Relation to use.
   * @param ids IDs to add
   * @return Covariance matrix
   */
  public static CovarianceMatrix make(Relation<? extends NumberVector> relation, DBIDs ids) {
    int dim = RelationUtil.dimensionality(relation);
    CovarianceMatrix c = new CovarianceMatrix(dim);
    double[] mean = c.mean;
    int count = 0;
    // Compute mean first:
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      for(int i = 0; i < dim; i++) {
        mean[i] += vec.doubleValue(i);
      }
      count++;
    }
    if(count == 0) {
      return c;
    }
    // Normalize mean
    for(int i = 0; i < dim; i++) {
      mean[i] /= count;
    }
    // Compute covariances second
    // Two-pass approach is numerically okay and fast, when possible.
    double[] tmp = c.nmea; // Scratch space
    double[][] elems = c.elements;
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      for(int i = 0; i < dim; i++) {
        tmp[i] = vec.doubleValue(i) - mean[i];
      }
      for(int i = 0; i < dim; i++) {
        for(int j = i; j < dim; j++) {
          elems[i][j] += tmp[i] * tmp[j];
        }
      }
    }
    // Restore symmetry.
    for(int i = 0; i < dim; i++) {
      for(int j = i + 1; j < dim; j++) {
        elems[j][i] = elems[i][j];
      }
    }
    c.wsum = count;
    return c;
  }
}
