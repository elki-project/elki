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
package elki.index.tree.betula.distance;

import elki.data.NumberVector;
import elki.index.tree.betula.features.BIRCHCF;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Priority;
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
 * @author Erich Schubert
 * @since 0.8.0
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf", //
    bibkey = "tr/wisc/Zhang97")
public class BIRCHAverageIntraclusterDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final BIRCHAverageIntraclusterDistance STATIC = new BIRCHAverageIntraclusterDistance();

  @Override
  public double squaredDistance(NumberVector v, ClusterFeature ocf) {
    if(!(ocf instanceof BIRCHCF)) {
      throw new IllegalStateException("This distance only supports BIRCH clustering features.");
    }
    BIRCHCF cf = (BIRCHCF) ocf;
    final int dim = v.getDimensionality();
    assert dim == cf.getDimensionality();
    final double div1 = 1. / (1 + cf.getWeight()), div2 = 1. / cf.getWeight();
    double sum = (cf.sumOfSumOfSquares() + BIRCHCF.sumOfSquares(v)) * div2;
    for(int d = 0; d < dim; d++) {
      double x = cf.ls(d) + v.doubleValue(d);
      sum -= (x * div1) * (x * div2);
    }
    return sum > 0 ? sum * 2. : 0;
  }

  @Override
  public double squaredDistance(ClusterFeature ocf1, ClusterFeature ocf2) {
    if(!(ocf1 instanceof BIRCHCF) || !(ocf2 instanceof BIRCHCF)) {
      throw new IllegalStateException("This distance only supports BIRCH clustering features.");
    }
    BIRCHCF cf1 = (BIRCHCF) ocf1;
    BIRCHCF cf2 = (BIRCHCF) ocf2;
    final int dim = cf1.getDimensionality();
    assert dim == cf2.getDimensionality();
    final int n12 = cf1.getWeight() + cf2.getWeight();
    final double div1 = 1. / n12, div2 = 1. / (n12 - 1);
    double sum = (cf1.sumOfSumOfSquares() + cf2.sumOfSumOfSquares()) * div2;
    for(int d = 0; d < dim; d++) {
      double x = cf1.ls(d) + cf2.ls(d);
      sum -= (x * div1) * (x * div2);
    }
    return sum > 0 ? sum * 2. : 0;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public BIRCHAverageIntraclusterDistance make() {
      return STATIC;
    }
  }
}
