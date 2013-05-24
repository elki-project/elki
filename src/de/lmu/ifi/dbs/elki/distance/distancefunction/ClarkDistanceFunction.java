package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Clark distance function for vector spaces.
 * 
 * Reference:
 * <p>
 * M.-M. Deza and E. Deza<br />
 * Dictionary of distances
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "M.-M. Deza and E. Deza", title = "Dictionary of distances", booktitle = "Dictionary of distances")
public class ClarkDistanceFunction extends AbstractSpatialDoubleDistanceFunction {
  /**
   * Static instance.
   */
  public static final ClarkDistanceFunction STATIC = new ClarkDistanceFunction();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC} instance instead.
   */
  @Deprecated
  public ClarkDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim1 = v1.getDimensionality();
    if (dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double sqsum = 0.;
    for (int d = 0; d < dim1; d++) {
      double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      double v = (xd - yd) / (Math.abs(xd) + Math.abs(yd));
      sqsum += v * v;
    }
    return Math.sqrt(sqsum / dim1);
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if (mbr1 instanceof NumberVector && mbr2 instanceof NumberVector) {
      return doubleDistance((NumberVector<?>) mbr1, (NumberVector<?>) mbr2);
    }
    final int dim1 = mbr1.getDimensionality();
    if (dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString() + "\n" + mbr1.getDimensionality() + "!=" + mbr2.getDimensionality());
    }
    double sqsum = 0.;
    for (int d = 0; d < dim1; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      final double diff;
      if (max1 < min2) {
        diff = min2 - max1;
      } else if (min1 > max2) {
        diff = min1 - max2;
      } else {
        // Minimum difference is 0
        continue;
      }
      final double absmax1 = Math.max(-min1, max1);
      final double absmax2 = Math.max(-min2, max2);
      // Division by 0 cannot happen: then diff = 0 and we continued above!
      double v = diff / (absmax1 + absmax2);
      sqsum += v * v;
    }
    return Math.sqrt(sqsum / dim1);
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
    protected ClarkDistanceFunction makeInstance() {
      return ClarkDistanceFunction.STATIC;
    }
  }
}
