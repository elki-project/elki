package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
 * Bray-Curtis distance function / Sørensen–Dice coefficient for continuous
 * spaces.
 * 
 * Reference:
 * <p>
 * J. R. Bray and J. T. Curtis<br />
 * An ordination of the upland forest communities of southern Wisconsin<br />
 * Ecological monographs 27.4
 * </p>
 * Also:
 * <p>
 * T. Sørensen<br />
 * A method of establishing groups of equal amplitude in plant sociology based
 * on similarity of species and its application to analyses of the vegetation on
 * Danish commons<br />
 * Kongelige Danske Videnskabernes Selskab 5 (4)
 * </p>
 * and:
 * <p>
 * L. R. Dice<br />
 * Measures of the Amount of Ecologic Association Between Species<br />
 * Ecology 26 (3)
 * </p>
 * 
 * 
 * Note: we modified the usual definition of Bray-Curtis for use with negative
 * values. In essence, this function is defined as:
 * 
 * ManhattanDistance(v1, v2) / (ManhattanNorm(v1) + ManhattanNorm(v2))
 * 
 * This obviously limits the usefulness of this distance function for cases
 * where this kind of normalization is desired. In particular in low dimensional
 * data it should be used with care.
 * 
 * TODO: add a version optimized for sparse vectors / binary data.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Alias({ "bray-curtis", "braycurtis", "sorensen", "dice", "sorensen-dice" })
@Reference(authors = "J. R. Bray and J. T. Curtis", title = "An ordination of the upland forest communities of southern Wisconsin", booktitle = "Ecological monographs 27.4", url = "http://dx.doi.org/10.2307/1942268")
public class BrayCurtisDistanceFunction extends AbstractSpatialDistanceFunction {
  /**
   * Static instance.
   */
  public static final BrayCurtisDistanceFunction STATIC_CONTINUOUS = new BrayCurtisDistanceFunction();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC_CONTINUOUS} instance instead.
   */
  @Deprecated
  public BrayCurtisDistanceFunction() {
    super();
  }

  /**
   * Dummy method, just to attach a second reference.
   */
  @Reference(authors = "T. Sørensen", title = "A method of establishing groups of equal amplitude in plant sociology based on similarity of species and its application to analyses of the vegetation on Danish commons", booktitle = "Kongelige Danske Videnskabernes Selskab 5 (4)")
  static void secondReference() {
    // Empty, just to attach a second reference
  };

  /**
   * Dummy method, just to attach a third reference.
   */
  @Reference(authors = "L. R. Dice", title = "Measures of the Amount of Ecologic Association Between Species", booktitle = "Ecology 26 (3)")
  static void thirdReference() {
    // Empty, just to attach a second reference
  };

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double sumdiff = 0., sumsum = 0.;
    for (int d = 0; d < dim; d++) {
      double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      sumdiff += Math.abs(xd - yd);
      sumsum += Math.abs(xd) + Math.abs(yd);
    }
    return sumdiff / sumsum;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double sumdiff = 0., sumsum = 0.;
    for (int d = 0; d < dim; d++) {
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
      return BrayCurtisDistanceFunction.STATIC_CONTINUOUS;
    }
  }
}
