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
package elki.distance.similarityfunction;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.database.query.distance.SpatialPrimitiveDistanceSimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.AbstractNumberVectorDistance;
import elki.distance.distancefunction.SpatialPrimitiveDistance;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Kulczynski similarity 1.
 * <p>
 * \[ s_\text{Kulczynski-1}(\vec{x},\vec{y}):=
 * \tfrac{\sum\nolimits_i\min\{x_i,y_i\}}{\sum\nolimits_i |x_i-y_i|} \]
 * or in distance form:
 * \[ d_\text{Kulczynski-1}(\vec{x},\vec{y}):=
 * \tfrac{\sum\nolimits_i |x_i-y_i|}{\sum\nolimits_i\min\{x_i,y_i\}} \]
 * <p>
 * Reference:
 * <p>
 * M.-M. Deza and E. Deza<br>
 * Dictionary of distances
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "M.-M. Deza, E. Deza", //
    title = "Dictionary of distances", //
    booktitle = "Dictionary of distances", //
    url = "https://doi.org/10.1007/978-3-642-00234-2", //
    bibkey = "doi:10.1007/978-3-642-00234-2")
public class Kulczynski1Similarity extends AbstractNumberVectorDistance implements SpatialPrimitiveDistance<NumberVector>, PrimitiveSimilarity<NumberVector> {
  /**
   * Static instance.
   */
  public static final Kulczynski1Similarity STATIC = new Kulczynski1Similarity();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC} instance instead.
   */
  @Deprecated
  public Kulczynski1Similarity() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double sumdiff = 0., summin = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      if(xd >= yd) {
        sumdiff += xd - yd;
        summin += yd;
      }
      else {
        sumdiff += yd - xd;
        summin += xd;
      }
    }
    return summin > 0 ? sumdiff / summin : 0.;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double sumdiff = 0., summin = 0.;
    for(int d = 0; d < dim; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      sumdiff += max1 < min2 ? min2 - max1 : min1 > max2 ? min1 - max2 : 0.;
      summin += min1 < min2 ? min1 : min2;
    }
    return summin > 0 ? sumdiff / summin : 0;
  }

  @Override
  public double similarity(NumberVector v1, NumberVector v2) {
    final int dim = AbstractNumberVectorDistance.dimensionality(v1, v2);
    double sumdiff = 0., summin = 0.;
    for(int i = 0; i < dim; i++) {
      double xi = v1.doubleValue(i), yi = v2.doubleValue(i);
      sumdiff += Math.abs(xi - yi);
      summin += Math.min(xi, yi);
    }
    return summin / sumdiff;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public <T extends NumberVector> SpatialPrimitiveDistanceSimilarityQuery<T> instantiate(Relation<T> database) {
    return new SpatialPrimitiveDistanceSimilarityQuery<>(database, this, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected Kulczynski1Similarity makeInstance() {
      return Kulczynski1Similarity.STATIC;
    }
  }
}
