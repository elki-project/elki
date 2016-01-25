package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Chi-Squared distance function, symmetric version.
 * 
 * Reference:
 * <p>
 * J. Puzicha, J.M. Buhmann, Y. Rubner, C. Tomasi<br />
 * Empirical evaluation of dissimilarity measures for color and texture<br />
 * Proc. 7th IEEE International Conference on Computer Vision
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Alias("chisq")
@Reference(authors = "J. Puzicha, J.M. Buhmann, Y. Rubner, C. Tomasi", //
title = "Empirical evaluation of dissimilarity measures for color and texture", //
booktitle = "Proc. 7th IEEE International Conference on Computer Vision", //
url = "http://dx.doi.org/10.1109/ICCV.1999.790412")
public class ChiSquaredDistanceFunction extends AbstractSpatialDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final ChiSquaredDistanceFunction STATIC = new ChiSquaredDistanceFunction();

  /**
   * Constructor for the Chi-Squared distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ChiSquaredDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double di = xd - yd;
      final double si = xd + yd;
      if(!(si > 0. || si < 0.) || !(di > 0. || di < 0.)) {
        continue;
      }
      agg += di * di / si;
    }
    for(int d = mindim; d < dim1; d++) {
      final double xd = v1.doubleValue(d);
      if(xd != xd) { /* avoid NaNs */
        continue;
      }
      agg += xd;
    }
    for(int d = mindim; d < dim2; d++) {
      final double xd = v2.doubleValue(d);
      if(xd != xd) { /* avoid NaNs */
        continue;
      }
      agg += xd;
    }
    return 2. * agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality(), dim2 = mbr2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      final double diff; // Minimum difference
      if(max1 < min2) {
        diff = min2 - max1;
      }
      else if(max2 < min1) {
        diff = max2 - min1;
      }
      else {
        continue; // 0.
      }
      final double si = max1 + max2; // Maximum sum
      if(!(si > 0. || si < 0.) || !(diff > 0. || diff < 0.)) {
        continue;
      }
      agg += diff * diff / si;
    }
    for(int d = mindim; d < dim1; d++) {
      final double min1 = mbr1.getMin(d);
      if(min1 > 0.) {
        agg += min1;
      }
      else {
        final double max1 = mbr1.getMax(d);
        if(max1 < 0.) { // Should never happen.
          agg += max1;
        }
      }
    }
    for(int d = mindim; d < dim2; d++) {
      final double min2 = mbr2.getMin(d);
      if(min2 > 0.) {
        agg += min2;
      }
      else {
        final double max2 = mbr2.getMax(d);
        if(max2 < 0.) { // Should never happen.
          agg += max2;
        }
      }
    }
    return 2. * agg;
  }

  @Override
  public String toString() {
    return "ChiSquaredDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ChiSquaredDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
