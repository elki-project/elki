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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Triangular Discrimination has relatively tight upper and lower bounds to the
 * Jensen-Shannon divergence, but is much less expensive.
 * <p>
 * \[\text{Triangular-Discrimination}(\vec{x},\vec{y}):=
 * \sum\nolimits_i \tfrac{|x_i-y_i|^2}{x_i+y_i}\]
 * <p>
 * This distance function is meant for distribution vectors that sum to 1, and
 * does not work on negative values.
 * <p>
 * See also {@link TriangularDistanceFunction} for a metric version.
 * <p>
 * Reference:
 * <p>
 * F. Topsøe<br>
 * Some inequalities for information divergence and related measures of
 * discrimination<br>
 * IEEE Transactions on information theory, 46(4).
 * <p>
 * TODO: support sparse vectors, varying length
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "F. Topsøe", //
    title = "Some inequalities for information divergence and related measures of discrimination", //
    booktitle = "IEEE Transactions on information theory, 46(4)", //
    url = "https://doi.org/10.1109/18.850703", //
    bibkey = "DBLP:journals/tit/Topsoe00")
public class TriangularDiscriminationDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector> {
  /**
   * Static instance. Use this!
   */
  public static final TriangularDiscriminationDistanceFunction STATIC = new TriangularDiscriminationDistanceFunction();

  /**
   * Constructor for the Triangular Discrimination - use {@link #STATIC}
   * instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public TriangularDiscriminationDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double sum = xd + yd;
      if(!(sum > 0.)) { // Avoid division by zero below.
        continue;
      }
      final double delta = xd - yd;
      agg += delta * delta / sum;
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
      final double sum = max1 + max2;
      if(!(sum > 0.)) {
        continue;
      }
      double delta = min2 - max1;
      delta = (delta >= 0.) ? delta : (min1 - max2);
      if(delta > 0.) {
        agg += delta * delta / sum;
      }
    }
    return agg;
  }

  @Override
  public boolean isSquared() {
    return true;
  }

  @Override
  public String toString() {
    return "TriangularDiscriminationDistanceFunction";
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected TriangularDiscriminationDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
