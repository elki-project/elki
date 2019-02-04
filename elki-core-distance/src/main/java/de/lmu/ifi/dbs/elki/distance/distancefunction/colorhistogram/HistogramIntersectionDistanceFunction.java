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
package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Intersection distance for color histograms.
 * <p>
 * Distance function for color histograms that emphasizes 'strong' bins.
 * <p>
 * Reference:
 * <p>
 * M. J. Swain, D. H. Ballard<br>
 * Color indexing<br>
 * International Journal of Computer Vision, 7(1), 32, 1991
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "M. J. Swain, D. H. Ballard", //
    title = "Color Indexing", //
    booktitle = "International Journal of Computer Vision, 7(1), 32, 1991", //
    url = "https://doi.org/10.1007/BF00130487", //
    bibkey = "DBLP:journals/ijcv/SwainB91")
public class HistogramIntersectionDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector> {
  /**
   * Static instance
   */
  public static final HistogramIntersectionDistanceFunction STATIC = new HistogramIntersectionDistanceFunction();

  /**
   * Constructor. No parameters.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public HistogramIntersectionDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0., norm1 = 0., norm2 = 0.;
    for(int i = 0; i < dim; i++) {
      final double val1 = v1.doubleValue(i), val2 = v2.doubleValue(i);
      agg += Math.min(val1, val2);
      norm1 += val1;
      norm2 += val2;
    }
    return 1. - agg / Math.min(norm1, norm2);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double agg = 0., norm1 = 0, norm2 = 0.;
    for(int i = 0; i < dim; i++) {
      final double min1 = mbr1.getMin(i), max1 = mbr1.getMax(i);
      final double min2 = mbr2.getMin(i), max2 = mbr2.getMax(i);
      agg += Math.min(max1, max2);
      norm1 += Math.max(0, min1);
      norm2 += Math.max(0, min2);
    }
    final double norm = Math.min(norm1, norm2);
    return norm > 0 && agg < norm ? (1 - agg / norm) : 0;
  }

  @Override
  public String toString() {
    return "HistogramIntersectionDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HistogramIntersectionDistanceFunction makeInstance() {
      return HistogramIntersectionDistanceFunction.STATIC;
    }
  }
}
