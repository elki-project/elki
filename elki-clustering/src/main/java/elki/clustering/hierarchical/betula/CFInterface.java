/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.hierarchical.betula;

import elki.data.NumberVector;

/**
 * Interface for basic ClusteringFeature functions
 * 
 * @author Andreas Lang
 */
public interface CFInterface extends HasCF {
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
  void addToStatistics(CFInterface other);

  /**
   * Resets all statistics of CF
   */
  void resetStatistics();

  /**
   * Returns the number of dimensions
   * 
   * @return dimensionality
   */
  int getDimensionality();

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

  /**
   * Squared distance of the centers.
   *
   * @param v Vector
   * @return sum of squared deviations from the center
   */
  double squaredCenterDistance(NumberVector v);

  /**
   * Squared distance of the centers.
   *
   * @param other Other clustering feature
   * @return sum of squared deviations from the center
   */
  double squaredCenterDistance(CFInterface other);

  /**
   * Absolute distance of the centers.
   *
   * @param v Vector
   * @return sum of squared deviations from the center
   */
  double absoluteCenterDistance(NumberVector v);

  /**
   * Absolute distance of the centers.
   *
   * @param other Other clustering feature
   * @return sum of squared deviations from the center
   */
  double absoluteCenterDistance(CFInterface other);

  @Override
  default CFInterface getCF() {
    return this;
  }
}
