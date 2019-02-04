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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

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
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/233269.233324", //
    bibkey = "DBLP:conf/sigmod/ZhangRL96")
public class RadiusCriterion implements BIRCHAbsorptionCriterion {
  /**
   * Static instance.
   */
  public static final RadiusCriterion STATIC = new RadiusCriterion();

  @Override
  public double squaredCriterion(ClusteringFeature f1, NumberVector n) {
    if(f1.n <= 0) {
      return 0.;
    }
    final int dim = f1.ls.length;
    final double div = 1. / (f1.n + 1);
    // Sum_d sum_i squares
    double sum = f1.sumOfSumOfSquares();
    for(int d = 0; d < dim; d++) {
      double v = n.doubleValue(d);
      sum += v * v;
    }
    sum *= div;
    // Sum_d square sum_i
    for(int d = 0; d < dim; d++) {
      double v = (f1.ls[d] + n.doubleValue(d)) * div;
      sum -= v * v;
    }
    return sum;
  }

  @Override
  public double squaredCriterion(ClusteringFeature f1, ClusteringFeature f2) {
    final int n12 = f1.n + f2.n;
    if(n12 <= 1) {
      return 0.;
    }
    final int dim = f1.ls.length;
    final double div = 1. / n12;
    double sum = (f1.sumOfSumOfSquares() + f2.sumOfSumOfSquares()) * div;
    for(int i = 0; i < dim; i++) {
      double v = (f1.ls[i] + f2.ls[i]) * div;
      sum -= v * v;
    }
    return sum;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected RadiusCriterion makeInstance() {
      return STATIC;
    }
  }
}
