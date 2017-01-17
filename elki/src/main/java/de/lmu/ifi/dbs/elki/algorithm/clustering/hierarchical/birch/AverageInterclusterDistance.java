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
 * Average intercluster distance.
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
@Alias({ "D2" })
@Reference(authors = "T. Zhang", //
    title = "Data Clustering for Very Large Datasets Plus Applications", //
    booktitle = "University of Wisconsin Madison, Technical Report #1355", //
    url = "ftp://ftp.cs.wisc.edu/pub/techreports/1997/TR1355.pdf")
public class AverageInterclusterDistance implements BIRCHDistance {
  /**
   * Static instance.
   */
  public static final AverageInterclusterDistance STATIC = new AverageInterclusterDistance();

  @Override
  public double distance(NumberVector v, ClusteringFeature cf) {
    final int d = v.getDimensionality();
    assert (d == cf.getDimensionality());
    // SS for new vector v:
    double ss = 0;
    for(int i = 0; i < d; i++) {
      double x = v.doubleValue(i);
      ss += x * x;
    }
    double sum = 0;
    for(int i = 0; i < d; i++) {
      sum += v.doubleValue(i) * cf.ls[i];
    }
    sum = /* 1. * */ cf.sumOfSumOfSquares() + cf.n * ss - 2 * sum;
    return sum > 0 ? Math.sqrt(sum / cf.n) : 0;
  }

  @Override
  public double distance(ClusteringFeature cf1, ClusteringFeature cf2) {
    final int d = cf1.getDimensionality();
    assert (d == cf2.getDimensionality());
    double sum = 0.;
    for(int i = 0; i < d; i++) {
      sum += cf1.ls[i] * cf2.ls[i];
    }
    sum = cf1.n * cf2.sumOfSumOfSquares() + cf2.n * cf1.sumOfSumOfSquares() - 2 * sum;
    return sum > 0 ? Math.sqrt(sum / (cf1.n * (long) cf2.n)) : 0;
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
    protected AverageInterclusterDistance makeInstance() {
      return STATIC;
    }
  }
}
