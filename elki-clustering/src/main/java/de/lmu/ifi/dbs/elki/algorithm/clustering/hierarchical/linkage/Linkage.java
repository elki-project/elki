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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Abstract interface for implementing a new linkage method into hierarchical
 * clustering.
 * <p>
 * Reference:
 * <p>
 * G. N. Lance, W. T. Williams<br>
 * A general theory of classificatory sorting strategies<br>
 * 1. Hierarchical systems<br>
 * The Computer Journal 9.4
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "G. N. Lance, W. T. Williams", //
    title = "A general theory of classificatory sorting strategies 1. Hierarchical systems", //
    booktitle = "The Computer Journal 9.4", //
    url = "https://doi.org/10.1093/comjnl/9.4.373", //
    bibkey = "doi:10.1093/comjnl/9.4.373")
public interface Linkage {
  /**
   * Initialization of the distance matrix.
   *
   * @param d Distance
   * @param issquare Flag to indicate the input values are already squared
   * @return Initial value
   */
  default double initial(double d, boolean issquare) {
    return d;
  }

  /**
   * Restore a distance to the original scale.
   *
   * @param d Distance
   * @param issquare Flag to indicate the input values were already squared
   * @return Initial value
   */
  default double restore(double d, boolean issquare) {
    return d;
  }

  /**
   * Compute combined linkage for two clusters.
   * 
   * @param sizex Size of first cluster x before merging
   * @param dx Distance of cluster x to j before merging
   * @param sizey Size of second cluster y before merging
   * @param dy Distance of cluster y to j before merging
   * @param sizej Size of candidate cluster j
   * @param dxy Distance between clusters x and y before merging
   * @return Combined distance
   */
  double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy);
}
