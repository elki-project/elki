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
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Bray-Curtis distance function for vector spaces.
 * 
 * Reference:
 * <p>
 * J. R. Bray and J. T. Curtis<br />
 * An ordination of the upland forest communities of southern Wisconsin<br />
 * Ecological monographs 27.4
 * </p>
 * 
 * Note: we modified the usual definition of Bray-Curtis for use with negative
 * values. In essence, this function is defined as:
 * 
 * ManhattanDistance(v1, v2) / (ManhattanNorm(v1) + ManhattanNorm(v2))
 * 
 * @author Erich Schubert
 */
@Alias({ "braycurtis", "sorensen" })
@Reference(authors = "J. R. Bray and J. T. Curtis", title = "An ordination of the upland forest communities of southern Wisconsin", booktitle = "Ecological monographs 27.4", url = "http://dx.doi.org/10.2307/1942268")
public class BrayCurtisDistanceFunction extends AbstractSpatialDoubleDistanceFunction {
  /**
   * Static instance.
   */
  public static final BrayCurtisDistanceFunction STATIC = new BrayCurtisDistanceFunction();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC} instance instead.
   */
  @Deprecated
  public BrayCurtisDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim1 = v1.getDimensionality();
    if (dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double sumdiff = 0., sumsum = 0.;
    for (int d = 0; d < dim1; d++) {
      double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      sumdiff += Math.abs(xd - yd);
      sumsum += Math.abs(xd) + Math.abs(yd);
    }
    return sumdiff / sumsum;
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
    double sumdiff = 0., sumsum = 0.;
    for (int d = 0; d < dim1; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      if (max1 < min2) {
        sumdiff += min2 - max1;
      } else if (min1 > max2) {
        sumdiff += min1 - max2;
      } else {
        // Minimum difference is 0
      }
      sumsum += Math.max(-min1, max1) + Math.max(-min2, max2);
    }
    return sumdiff / sumsum;
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
    protected BrayCurtisDistanceFunction makeInstance() {
      return BrayCurtisDistanceFunction.STATIC;
    }
  }
}
