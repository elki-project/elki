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
package elki.clustering.hierarchical.linkage;

/**
 * Geometric linkages, in addition to the combination with
 * Lance-Williams-Equations, these linkages can also be computed by aggregating
 * data points (for vector data only).
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public interface GeometricLinkage extends Linkage {
  /**
   * Merge the aggregated vectors.
   * 
   * @param x Center of the first cluster
   * @param sizex Weight of the first cluster
   * @param y Center of the second cluster
   * @param sizey Weight of the second cluster
   * @return Combined vector
   */
  public double[] merge(double[] x, int sizex, double[] y, int sizey);

  /**
   * Distance of two aggregated clusters.
   * 
   * @param x Center of the first cluster
   * @param sizex Weight of the first cluster
   * @param y Center of the second cluster
   * @param sizey Weight of the second cluster
   * @return Distance
   */
  public double distance(double[] x, int sizex, double[] y, int sizey);
}
