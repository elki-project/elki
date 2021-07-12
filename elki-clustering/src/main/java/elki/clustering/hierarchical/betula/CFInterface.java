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
 * 
 * @param <T> ClusteringFeature
 */
public interface CFInterface {

  /**
   * Add NumberVector to CF
   * 
   * @param nv NumberVector
   */
  public void addToStatistics(NumberVector nv);

  /**
   * Add other CF to CF
   * 
   * @param other other CF
   */
  public void addToStatistics(CFInterface other);

  /**
   * resets all Statistics of CF
   */
  public void resetStatistics();

  /**
   * Number of dimensions
   * 
   * @return dimensionality
   */
  public int getDimensionality();

  /**
   * 
   * weight
   * 
   * @return weight of CF
   */
  public int getWeight();

  /**
   * Returns the mean of the specified dimension.
   * 
   * @param d dimension
   * @return mean of this dimension
   */
  public double centroid(int d);

  /**
   * Returns the total variance.
   * 
   * @return variance.
   */
  public double variance();

  /**
   * Returns the total sum of Deviations.
   * 
   * @return Sum of Deviances.
   */
  public double SoD();

  /**
   * Returns the variance in the specified dimension.
   * 
   * @param d dimension
   * @return variance in this dimension.
   */
  public double variance(int d);

  /**
   * returns the covariance matrix
   * 
   * @return covariance
   */
  public double[][] covariance();

}
