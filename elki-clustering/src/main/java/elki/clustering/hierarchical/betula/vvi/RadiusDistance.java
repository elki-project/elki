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
package elki.clustering.hierarchical.betula.vvi;

import elki.data.NumberVector;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Average Radius (R) criterion.
 * <p>
 * References:
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: An Efficient Data Clustering Method for Very Large Databases<br>
 * Proc. 1996 ACM SIGMOD International Conference on Management of Data
 *
 * @author Andreas Lang
 */
@Alias("R")
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/233269.233324", //
    bibkey = "DBLP:conf/sigmod/ZhangRL96")
public class RadiusDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final RadiusDistance STATIC = new RadiusDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusteringFeature cf1) {
    return cf1.n <= 0 ? 0 : //
        (cf1.n / (cf1.n + 1.) * cf1.squaredCenterDistance(nv) + cf1.sumdev()) / (cf1.n + 1.);
  }

  @Override
  public double squaredDistance(ClusteringFeature cf1, ClusteringFeature cf2) {
    final double n1 = cf1.n, n2 = cf2.n, n12 = n1 + n2;
    return n12 <= 0 ? 0 : //
        (n1 * n2 / n12 * cf1.squaredCenterDistance(cf2) + cf1.sumdev() + cf2.sumdev()) / n12;
  }

  /**
   * Parameterization class
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    @Override
    public RadiusDistance make() {
      return STATIC;
    }
  }
}
