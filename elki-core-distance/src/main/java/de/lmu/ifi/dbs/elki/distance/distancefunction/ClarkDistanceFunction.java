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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Clark distance function for vector spaces.
 * <p>
 * Clark distance is defined as:
 * \[ \text{Clark}(\vec{x},\vec{y}) :=
 * \sqrt{\tfrac{1}{d}\sum\nolimits_i \left(\tfrac{|x_i-y_i|}{|x_i|+|y_i|}\right)^2} \]
 * <p>
 * Reference:
 * <p>
 * M.-M. Deza, E. Deza<br>
 * Dictionary of distances
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Priority(Priority.RECOMMENDED)
@Reference(authors = "M.-M. Deza, E. Deza", //
    title = "Dictionary of distances", //
    booktitle = "Dictionary of distances", //
    url = "https://doi.org/10.1007/978-3-642-00234-2", //
    bibkey = "doi:10.1007/978-3-642-00234-2")
public class ClarkDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, NumberVectorDistanceFunction<NumberVector> {
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
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double div = Math.abs(xd) + Math.abs(yd);
      if(div > 0.) {
        final double v = (xd - yd) / div;
        agg += v * v;
      }
    }
    for(int d = mindim; d < dim1; d++) {
      if(v1.doubleValue(d) != 0) {
        agg += 1;
      }
    }
    for(int d = mindim; d < dim2; d++) {
      if(v2.doubleValue(d) != 0) {
        agg += 1;
      }
    }
    return FastMath.sqrt(agg / Math.max(dim1, dim2));
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality(), dim2 = mbr2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      final double diff;
      if(max1 < min2) {
        diff = min2 - max1;
      }
      else if(min1 > max2) {
        diff = min1 - max2;
      }
      else {
        // Minimum difference is 0
        continue;
      }
      final double absmax1 = Math.max(-min1, max1);
      final double absmax2 = Math.max(-min2, max2);
      // Division by 0 cannot happen: then diff = 0 and we continued above!
      final double v = diff / (absmax1 + absmax2);
      agg += v * v;
    }
    for(int d = mindim; d < dim1; d++) {
      if(mbr1.getMin(d) > 0. || mbr1.getMax(d) < 0.) {
        agg += 1;
      }
    }
    for(int d = mindim; d < dim2; d++) {
      if(mbr2.getMin(d) > 0. || mbr2.getMax(d) < 0.) {
        agg += 1;
      }
    }
    return FastMath.sqrt(agg / Math.max(dim1, dim2));
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
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
    protected ClarkDistanceFunction makeInstance() {
      return ClarkDistanceFunction.STATIC;
    }
  }
}
