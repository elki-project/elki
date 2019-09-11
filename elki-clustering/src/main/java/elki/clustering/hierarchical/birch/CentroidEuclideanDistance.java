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
package elki.clustering.hierarchical.birch;

import elki.data.NumberVector;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Centroid Euclidean distance.
 * <p>
 * Reference:
 * <p>
 * Data Clustering for Very Large Datasets Plus Applications<br>
 * T. Zhang<br>
 * Doctoral Dissertation, 1997.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Alias({ "D0" })
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf", //
    bibkey = "tr/wisc/Zhang97")
public class CentroidEuclideanDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final CentroidEuclideanDistance STATIC = new CentroidEuclideanDistance();

  @Override
  public double squaredDistance(NumberVector v, ClusteringFeature cf) {
    final int d = v.getDimensionality();
    assert (d == cf.getDimensionality());
    double sum = 0.;
    for(int i = 0; i < d; i++) {
      double dx = cf.centroid(i) - v.doubleValue(i);
      sum += dx * dx;
    }
    return sum;
  }

  @Override
  public double squaredDistance(ClusteringFeature v, ClusteringFeature cf) {
    final int d = v.getDimensionality();
    assert (d == cf.getDimensionality());
    double sum = 0.;
    for(int i = 0; i < d; i++) {
      double dx = cf.centroid(i) - v.centroid(i);
      sum += dx * dx;
    }
    return sum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public CentroidEuclideanDistance make() {
      return STATIC;
    }
  }
}
