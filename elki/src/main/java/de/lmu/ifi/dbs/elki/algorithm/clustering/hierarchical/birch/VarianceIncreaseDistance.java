package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Variance increase distance.
 * 
 * Reference:
 * <p>
 * Data Clustering for Very Large Datasets Plus Applications<br />
 * T. Zhang<br />
 * Doctoral Dissertation, 1997.
 * </p>
 * 
 * @author Erich Schubert
 */
@Alias({ "D4" })
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf")
public class VarianceIncreaseDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final VarianceIncreaseDistance STATIC = new VarianceIncreaseDistance();

  @Override
  public double distance(NumberVector v, ClusteringFeature cf) {
    final int d = v.getDimensionality();
    assert (d == cf.getDimensionality());
    double ss2 = cf.sumOfSumOfSquares();
    int n2 = cf.n, n12 = 1 + n2;
    double sum12 = 0., sum1 = 0., sum2 = 0.;
    for(int i = 0; i < d; i++) {
      double x1 = v.doubleValue(i), x2 = cf.ls[i];
      double x12 = (x1 + x2) / n12;
      sum12 += x12 * x12;
      // n1 is 1.
      sum1 += x1 * x1;
      x2 = x2 / n2;
      sum2 += x2 * x2;
    }
    double sum = (sum1 + ss2) / n12 - sum12 //
        + ss2 / n2 - sum2;
    return sum > 0 ? Math.sqrt(sum) : 0;
  }

  @Override
  public double distance(ClusteringFeature cf1, ClusteringFeature cf2) {
    final int d = cf1.getDimensionality();
    assert (d == cf2.getDimensionality());
    double ss1 = cf1.sumOfSumOfSquares(), ss2 = cf2.sumOfSumOfSquares();
    int n1 = cf1.n, n2 = cf2.n, n12 = n1 + n2;
    double sum12 = 0., sum1 = 0., sum2 = 0.;
    for(int i = 0; i < d; i++) {
      double x1 = cf1.ls[i], x2 = cf2.ls[i];
      double x12 = (x1 + x2) / n12; // E(x)
      sum12 += x12 * x12; // sum E(x)^2
      x1 = x1 / n1; // E(x)
      sum1 += x1 * x1; // sum E(x)^2
      x2 = x2 / n2; // E(x)
      sum2 += x2 * x2; // sum E(x)^2
    }
    double sum = (ss1 + ss2) / n12 - sum12 //
        + ss1 / n1 - sum1 //
        + ss2 / n2 - sum2;
    return sum > 0 ? Math.sqrt(sum) : 0;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected VarianceIncreaseDistance makeInstance() {
      return STATIC;
    }
  }
}
