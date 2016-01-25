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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Lorentzian distance function for vector spaces.
 * 
 * Reference:
 * <p>
 * M.-M. Deza and E. Deza<br />
 * Dictionary of distances
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "M.-M. Deza and E. Deza", title = "Dictionary of distances", booktitle = "Dictionary of distances")
public class LorentzianDistanceFunction extends AbstractSpatialNorm {
  /**
   * Static instance.
   */
  public static final LorentzianDistanceFunction STATIC = new LorentzianDistanceFunction();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC} instance instead.
   */
  @Deprecated
  public LorentzianDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      agg += Math.log(1 + Math.abs(xd - yd));
    }
    return agg;
  }

  @Override
  public double norm(NumberVector v1) {
    final int dim = v1.getDimensionality();
    double agg = 0.;
    for (int i = 0; i < dim; i++) {
      final double xi = v1.doubleValue(i);
      agg += Math.log(1. + Math.abs(xi));
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
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
      agg += Math.log(1. + diff);
    }
    return agg;
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
    protected LorentzianDistanceFunction makeInstance() {
      return LorentzianDistanceFunction.STATIC;
    }
  }
}
