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
 * Average intracluster distance.
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
@Alias({ "D3" })
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf")
public class AverageIntraclusterDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final AverageIntraclusterDistance STATIC = new AverageIntraclusterDistance();

  @Override
  public double distance(NumberVector v, ClusteringFeature cf) {
    final int d = v.getDimensionality();
    assert (d == cf.getDimensionality());
    final int n12 = 1 + cf.n;
    double sum = cf.sumOfSumOfSquares();
    for(int i = 0; i < d; i++) {
      double x = v.doubleValue(i);
      sum += x * x;
    }
    sum *= n12;
    for(int i = 0; i < d; i++) {
      double x = cf.ls[i] + v.doubleValue(i);
      sum -= x * x;
    }
    double sqd = sum / (0.5 * n12 * (n12 - 1.));
    return sqd > 0 ? Math.sqrt(sqd) : 0;
  }

  @Override
  public double distance(ClusteringFeature cf1, ClusteringFeature cf2) {
    final int d = cf1.getDimensionality();
    assert (d == cf2.getDimensionality());
    final int n12 = cf1.n + cf2.n;
    double sum = n12 * (cf1.sumOfSumOfSquares() + cf2.sumOfSumOfSquares());
    for(int i = 0; i < d; i++) {
      double v = cf1.ls[i] + cf2.ls[i];
      sum -= v * v;
    }
    double sqd = sum / (0.5 * n12 * (n12 - 1.));
    return sqd > 0 ? Math.sqrt(sqd) : 0.;
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
    protected AverageIntraclusterDistance makeInstance() {
      return STATIC;
    }
  }
}
