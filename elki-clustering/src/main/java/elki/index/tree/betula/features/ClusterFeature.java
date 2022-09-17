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
package elki.index.tree.betula.features;

import elki.data.NumberVector;

/**
 * Interface for basic ClusteringFeature functions
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
public interface ClusterFeature extends AsClusterFeature, NumberVector {
  /**
   * Add NumberVector to CF
   * 
   * @param nv NumberVector
   */
  void addToStatistics(NumberVector nv);

  /**
   * Add other CF to CF
   * 
   * @param other other CF
   */
  void addToStatistics(ClusterFeature other);

  /**
   * Resets all statistics of CF
   */
  void resetStatistics();

  /**
   * Return the weight
   * 
   * @return weight of CF
   */
  int getWeight();

  /**
   * Returns the mean of the specified dimension.
   * 
   * @param d dimension
   * @return mean of this dimension
   */
  double centroid(int d);

  @Override
  default double doubleValue(int dimension) {
    return centroid(dimension);
  }

  @Override
  default long longValue(int dimension) {
    return (long) centroid(dimension);
  }

  /**
   * Returns the total variance.
   * 
   * @return variance.
   */
  double variance();

  /**
   * Returns the total sum of Deviations.
   * 
   * @return Sum of Deviations.
   */
  double sumdev();

  /**
   * Returns the variance in the specified dimension.
   * 
   * @param d dimension
   * @return variance in this dimension.
   */
  double variance(int d);

  /**
   * returns the covariance matrix
   * 
   * @return covariance
   */
  double[][] covariance();

  @Override
  default ClusterFeature getCF() {
    return this;
  }

  /**
   * Cluster feature factory
   *
   * @author Erich Schubert
   * 
   * @param <F> feature type
   */
  static interface Factory<F extends ClusterFeature> {
    /**
     * Make a new clustering feature of the given dimensionality.
     * 
     * @param dim Dimensionality
     * @return Clustering feature
     */
    F make(int dim);
  }
}
