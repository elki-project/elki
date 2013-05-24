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
 * @author Erich Schubert
 */
@Reference(authors = "J. R. Bray and J. T. Curtis", title = "An ordination of the upland forest communities of southern Wisconsin", booktitle = "Ecological monographs 27.4", url = "http://dx.doi.org/10.2307/1942268")
public class BrayCurtisDistanceFunction extends AbstractVectorDoubleDistanceFunction {
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
    for (int i = 0; i < dim1; i++) {
      double xi = v1.doubleValue(i), yi = v2.doubleValue(i);
      sumdiff += Math.abs(xi - yi);
      sumsum += xi + yi;
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
