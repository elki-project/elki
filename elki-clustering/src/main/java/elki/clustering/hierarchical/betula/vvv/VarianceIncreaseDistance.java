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
 * Variance increase distance.
 * <p>
 * Reference:
 * <p>
 * Data Clustering for Very Large Datasets Plus Applications<br>
 * T. Zhang<br>
 * Doctoral Dissertation, 1997.
 *
 * @author Andreas Lang
 */
@Alias({ "D4" })
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf", //
    bibkey = "tr/wisc/Zhang97")
public class VarianceIncreaseDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final VarianceIncreaseDistance STATIC = new VarianceIncreaseDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusteringFeature cf) {
    return cf.squaredCenterDistance(nv) * cf.n / (cf.n + 1);
  }

  @Override
  public double squaredDistance(ClusteringFeature cf1, ClusteringFeature cf2) {
    return cf1.squaredCenterDistance(cf2) * cf1.n * cf2.n / (cf1.n + cf2.n);
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    @Override
    public VarianceIncreaseDistance make() {
      return STATIC;
    }
  }
}
