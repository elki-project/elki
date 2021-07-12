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
package elki.clustering.hierarchical.betula.vii;

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
 *
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
    if(cf1.n <= 0) {
      return 0.;
    }
    double diameter = 0.;
    double deltas = 0.;
    double n = cf1.n + 1.;
    for(int i = 0; i < nv.getDimensionality(); i++) {
      double delta = cf1.centroid(i) - nv.doubleValue(i);
      deltas += delta * delta;
    }
    diameter = n * cf1.ssd + cf1.n * deltas;
    return diameter > 0 ? (2 * diameter) / (n * (n - 1.)) : 0.;
  }

  @Override
  public double squaredDistance(ClusteringFeature cf1, ClusteringFeature cf2) {
    double n12 = cf1.n + cf2.n;
    if(n12 <= 0) {
      return 0.;
    }
    double diameter = 0.;
    double deltas = 0.;
    for(int i = 0; i < cf1.getDimensionality(); i++) {
      double delta = cf1.centroid(i) - cf2.centroid(i);
      deltas += delta * delta;
    }
    diameter = n12 * (cf1.ssd + cf2.ssd) + cf1.n * cf2.n * deltas;
    return diameter > 0 ? (2 * diameter) / (n12 * (n12 - 1.)) : 0.;
  }

  @Override
  public double matSelfInit(ClusteringFeature cf) {
    return (2 * cf.sumOfSquaredDev()) / (cf.n - 1);
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
