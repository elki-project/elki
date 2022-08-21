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
 * Variance increase distance.
 * <p>
 * Reference:
 * <p>
 * Data Clustering for Very Large Datasets Plus Applications<br>
 * T. Zhang<br>
 * Doctoral Dissertation, 1997.
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
public class BIRCHVarianceIncreaseDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final BIRCHVarianceIncreaseDistance STATIC = new BIRCHVarianceIncreaseDistance();

  @Override
  public double squaredDistance(NumberVector v, ClusterFeature ocf) {
    if(!(ocf instanceof BIRCHCF)) {
      throw new IllegalStateException("This distance only supports BIRCH clustering features.");
    }
    BIRCHCF cf = (BIRCHCF) ocf;
    final int dim = v.getDimensionality();
    assert dim == cf.getDimensionality();
    final int n2 = cf.getWeight(), n3 = 1 + n2;
    final double div2 = 1. / n2, div3 = 1. / n3;
    double dot1 = 0., dot2 = 0., dot3 = 0.;
    for(int d = 0; d < dim; d++) {
      double x1 = v.doubleValue(d), x2 = cf.ls(d);
      double x3 = (x1 + x2) * div3;
      x2 *= div2;
      // x1 *= div1;
      dot1 += x1 * x1;
      dot2 += x2 * x2;
      dot3 += x3 * x3;
    }
    double sum = /* 1 * */ dot1 + n2 * dot2 - n3 * dot3;
    return sum > 0 ? sum : 0;
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
    final int n1 = cf1.getWeight(), n2 = cf2.getWeight(), n3 = n1 + n2;
    final double div1 = 1. / n1, div2 = 1. / n2, div3 = 1. / n3;
    double dot1 = 0., dot2 = 0., dot3 = 0.;
    for(int d = 0; d < dim; d++) {
      double x1 = cf1.ls(d), x2 = cf2.ls(d);
      double x3 = (x1 + x2) * div3;
      x2 *= div2;
      x1 *= div1;
      dot1 += x1 * x1;
      dot2 += x2 * x2;
      dot3 += x3 * x3;
    }
    double sum = n1 * dot1 + n2 * dot2 - n3 * dot3;
    return sum > 0 ? sum : 0;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public BIRCHVarianceIncreaseDistance make() {
      return STATIC;
    }
  }
}
