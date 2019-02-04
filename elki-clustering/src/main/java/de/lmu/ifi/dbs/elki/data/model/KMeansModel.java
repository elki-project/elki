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
package de.lmu.ifi.dbs.elki.data.model;

/**
 * Trivial subclass of the {@link MeanModel} that indicates the clustering to be
 * produced by k-means (so the Voronoi cell visualization is sensible).
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public class KMeansModel extends MeanModel {
  /**
   * Variance sum.
   */
  double varsum;

  /**
   * Constructor with mean.
   * 
   * @param mean Mean vector.
   * @param varsum Variance sum.
   */
  public KMeansModel(double[] mean, double varsum) {
    super(mean);
    this.varsum = varsum;
  }

  /**
   * Get the variance contribution of the cluster (sum of variances)
   * 
   * @return Sum of in-cluster variance
   */
  public double getVarianceContribution() {
    return varsum;
  }
}
