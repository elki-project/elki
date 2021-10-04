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
package elki.clustering.hierarchical.betula.birch;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.clustering.hierarchical.betula.distance.CFDistance;
import elki.data.NumberVector;
import elki.utilities.Alias;
import elki.utilities.Priority;
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
 * @author Erich Schubert
 * @since 0.7.5
 */
@Alias("R")
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/233269.233324", //
    bibkey = "DBLP:conf/sigmod/ZhangRL96")
public class BIRCHRadiusDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final BIRCHRadiusDistance STATIC = new BIRCHRadiusDistance();

  @Override
  public double squaredDistance(NumberVector n, CFInterface ocf) {
    if(!(ocf instanceof ClusteringFeature)) {
      throw new IllegalStateException("This distance only supports BIRCH clustering features.");
    }
    ClusteringFeature cf = (ClusteringFeature) ocf;
    if(cf.getWeight() <= 0) {
      return 0.;
    }
    final int dim = cf.ls.length;
    final double div = 1. / (cf.getWeight() + 1);
    // Sum_d sum_i squares
    double sum = cf.sumOfSumOfSquares();
    for(int d = 0; d < dim; d++) {
      double v = n.doubleValue(d);
      sum += v * v;
    }
    sum *= div;
    // Sum_d square sum_i
    for(int d = 0; d < dim; d++) {
      double v = (cf.ls[d] + n.doubleValue(d)) * div;
      sum -= v * v;
    }
    return sum > 0 ? sum : 0;
  }

  @Override
  public double squaredDistance(CFInterface ocf1, CFInterface ocf2) {
    if(!(ocf1 instanceof ClusteringFeature) || !(ocf2 instanceof ClusteringFeature)) {
      throw new IllegalStateException("This distance only supports BIRCH clustering features.");
    }
    ClusteringFeature cf1 = (ClusteringFeature) ocf1;
    ClusteringFeature cf2 = (ClusteringFeature) ocf2;
    final int n12 = cf1.getWeight() + cf2.getWeight();
    if(n12 <= 1) {
      return 0.;
    }
    final int dim = cf1.ls.length;
    final double div = 1. / n12;
    double sum = (cf1.sumOfSumOfSquares() + cf2.sumOfSumOfSquares()) * div;
    for(int i = 0; i < dim; i++) {
      double v = (cf1.ls[i] + cf2.ls[i]) * div;
      sum -= v * v;
    }
    return sum > 0 ? sum : 0;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public BIRCHRadiusDistance make() {
      return STATIC;
    }
  }
}
