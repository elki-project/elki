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
 * Average intercluster distance.
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
public class BIRCHAverageInterclusterDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final BIRCHAverageInterclusterDistance STATIC = new BIRCHAverageInterclusterDistance();

  @Override
  public double squaredDistance(NumberVector v, ClusterFeature ocf) {
    if(!(ocf instanceof BIRCHCF)) {
      throw new IllegalStateException("This distance only supports BIRCH clustering features.");
    }
    BIRCHCF cf = (BIRCHCF) ocf;
    final int dim = v.getDimensionality();
    assert dim == cf.getDimensionality();
    // Dot product:
    double sum = 0;
    for(int d = 0; d < dim; d++) {
      sum += v.doubleValue(d) * cf.ls(d);
    }
    sum = /* 1. * */ cf.sumdev() + cf.getWeight() * BIRCHCF.sumOfSquares(v) - 2 * sum;
    return sum > 0 ? sum / cf.getWeight() : 0;
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
    // Dot product:
    double sum = 0;
    for(int d = 0; d < dim; d++) {
      sum += cf1.ls(d) * cf2.ls(d);
    }
    sum = cf2.getWeight() * cf1.sumOfSumOfSquares() + cf1.getWeight() * cf2.sumOfSumOfSquares() - 2 * sum;
    return sum > 0 ? sum / (cf1.getWeight() * (long) cf2.getWeight()) : 0;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public BIRCHAverageInterclusterDistance make() {
      return STATIC;
    }
  }
}
