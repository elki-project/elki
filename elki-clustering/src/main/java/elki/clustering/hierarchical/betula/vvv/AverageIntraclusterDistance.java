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
package elki.clustering.hierarchical.betula.vvv;

import elki.data.NumberVector;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Average intracluster distance.
 * <p>
 * Reference:
 * <p>
 * Data Clustering for Very Large Datasets Plus Applications<br>
 * T. Zhang<br>
 * Doctoral Dissertation, 1997.
 * <p>
 * Note: this distance did not work well in the original work, apparently.
 *
 * @author Andreas Lang
 */
@Alias({ "D3" })
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf", //
    bibkey = "tr/wisc/Zhang97")
public class AverageIntraclusterDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final AverageIntraclusterDistance STATIC = new AverageIntraclusterDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusteringFeature cf1) {
    return cf1.n <= 0 ? 0 : //
        2 * ((cf1.n + 1) * cf1.sumdev() + cf1.n * cf1.squaredCenterDistance(nv)) / ((cf1.n + 1) * cf1.n);
  }

  @Override
  public double squaredDistance(ClusteringFeature cf1, ClusteringFeature cf2) {
    final double n12 = cf1.n + cf2.n;
    return n12 <= 0 ? 0 : //
        2 * (n12 * (cf1.sumdev() + cf2.sumdev()) + cf1.n * cf2.n * cf1.squaredCenterDistance(cf2)) / (n12 * (n12 - 1));
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    @Override
    public AverageIntraclusterDistance make() {
      return STATIC;
    }
  }
}
