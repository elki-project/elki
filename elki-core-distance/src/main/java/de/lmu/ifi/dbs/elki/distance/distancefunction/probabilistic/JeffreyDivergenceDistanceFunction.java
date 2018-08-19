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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Jeffrey Divergence Distance for {@link NumberVector}s.
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
@Reference(authors = "J. Puzicha, J.M. Buhmann, Y. Rubner, C. Tomasi", //
title = "Empirical evaluation of dissimilarity measures for color and texture", //
booktitle = "Proc. 7th IEEE International Conference on Computer Vision", //
url = "https://doi.org/10.1109/ICCV.1999.790412")
public class JeffreyDivergenceDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, NumberVectorDistanceFunction<NumberVector> {
  /**
   * Static instance. Use this!
   */
  public static final JeffreyDivergenceDistanceFunction STATIC = new JeffreyDivergenceDistanceFunction();

  /**
   * Constructor for the Jeffrey divergence - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public JeffreyDivergenceDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      if(xd == yd) {
        continue;
      }
      final double md = .5 * (xd + yd);
      if(!(md > 0. || md < 0.)) {
        continue;
      }
      if(xd > 0.) {
        agg += xd * FastMath.log(xd / md);
      }
      if(yd > 0.) {
        agg += yd * FastMath.log(yd / md);
      }
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double agg = 0;
    for(int d = 0; d < dim; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      final double md = .5 * (max1 + max2);
      if(!(md > 0. || md < 0.)) {
        continue;
      }
      if(min1 > 0.) {
        agg += min1 * FastMath.log(min1 / md);
      }
      if(min2 > 0.) {
        agg += min2 * FastMath.log(min2 / md);
      }
    }
    return agg;
  }

  @Override
  public String toString() {
    return "JeffreyDivergenceDistance";
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
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected JeffreyDivergenceDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
