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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Bray-Curtis distance function / Sørensen–Dice coefficient for continuous
 * vector spaces (not only binary data).
 * <p>
 * Reference:
 * <p>
 * J. R. Bray, J. T. Curtis<br>
 * An ordination of the upland forest communities of southern Wisconsin<br>
 * Ecological monographs 27.4
 * <p>
 * Also:
 * <p>
 * T. Sørensen<br>
 * A method of establishing groups of equal amplitude in plant sociology based
 * on similarity of species and its application to analyses of the vegetation on
 * Danish commons<br>
 * Kongelige Danske Videnskabernes Selskab 5 (4)
 * <p>
 * and:
 * <p>
 * L. R. Dice<br>
 * Measures of the Amount of Ecologic Association Between Species<br>
 * Ecology 26 (3)
 * <p>
 * Note: we modified the usual definition of Bray-Curtis for use with negative
 * values. In essence, this function is defined as:
 * <p>
 * ManhattanDistance(v1, v2) / (ManhattanNorm(v1) + ManhattanNorm(v2))
 * <p>
 * This obviously limits the usefulness of this distance function for cases
 * where this kind of normalization is desired. In particular in low dimensional
 * data it should be used with care.
 * <p>
 * TODO: add a version <i>optimized</i> for sparse vectors / binary data.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. R. Bray, J. T. Curtis", //
    title = "An ordination of the upland forest communities of southern Wisconsin", //
    booktitle = "Ecological monographs 27.4", //
    url = "https://doi.org/10.2307/1942268", //
    bibkey = "doi:10.2307/1942268")
@Reference(authors = "T. Sørensen", //
    title = "A method of establishing groups of equal amplitude in plant sociology based on similarity of species and its application to analyses of the vegetation on Danish commons", //
    booktitle = "Kongelige Danske Videnskabernes Selskab 5 (4)", //
    bibkey = "journals/misc/Sorensen48")
@Reference(authors = "L. R. Dice", //
    title = "Measures of the Amount of Ecologic Association Between Species", //
    booktitle = "Ecology 26 (3)", //
    url = "https://doi.org/10.2307/1932409", //
    bibkey = "doi:10.2307/1932409")
@Alias({ "bray-curtis", "braycurtis", "sorensen", "dice", "sorensen-dice" })
public class BrayCurtisDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector> {
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

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double sumdiff = 0., sumsum = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      sumdiff += Math.abs(xd - yd);
      sumsum += Math.abs(xd) + Math.abs(yd);
    }
    return sumsum > 0 ? sumdiff / sumsum : 0;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double sumdiff = 0., sumsum = 0.;
    for(int d = 0; d < dim; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      sumdiff += (max1 < min2) ? min2 - max1 : (min1 > max2) ? min1 - max2 : 0;
      sumsum += Math.max(-min1, max1) + Math.max(-min2, max2);
    }
    return sumsum > 0 ? sumdiff / sumsum : 0;
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
    protected BrayCurtisDistanceFunction makeInstance() {
      return BrayCurtisDistanceFunction.STATIC_CONTINUOUS;
    }
  }
}
