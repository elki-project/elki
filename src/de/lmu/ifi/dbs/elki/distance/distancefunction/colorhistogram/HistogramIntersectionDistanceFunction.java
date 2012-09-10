package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Intersection distance for color histograms.
 * 
 * According to: M. J. Swain, D. H. Ballard:<br />
 * Color indexing<br />
 * International Journal of Computer Vision, 7(1), 32, 1991
 * 
 * @author Erich Schubert
 */
@Title("Color histogram intersection distance")
@Description("Distance function for color histograms that emphasizes 'strong' bins.")
@Reference(authors = "M. J. Swain, D. H. Ballard", title = "Color Indexing", booktitle = "International Journal of Computer Vision, 7(1), 32, 1991")
public class HistogramIntersectionDistanceFunction extends AbstractVectorDoubleDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?>> {
  /**
   * Static instance
   */
  public static final HistogramIntersectionDistanceFunction STATIC = new HistogramIntersectionDistanceFunction();

  /**
   * Constructor. No parameters.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public HistogramIntersectionDistanceFunction() {
    super();
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double dist = 0;
    double norm1 = 0;
    double norm2 = 0;
    for(int i = 1; i <= dim1; i++) {
      final double val1 = v1.doubleValue(i);
      final double val2 = v2.doubleValue(i);
      dist += Math.min(val1, val2);
      norm1 += val1;
      norm2 += val2;
    }
    dist = 1 - dist / Math.min(norm1, norm2);
    return dist;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString() + "\n" + mbr1.getDimensionality() + "!=" + mbr2.getDimensionality());
    }
    double dist = 0;
    double norm1 = 0;
    double norm2 = 0;
    for(int i = 1; i <= dim1; i++) {
      final double min1 = mbr1.getMin(i);
      final double max1 = mbr1.getMax(i);
      final double min2 = mbr2.getMin(i);
      final double max2 = mbr2.getMax(i);
      dist += Math.min(max1, max2);
      norm1 += min1;
      norm2 += min2;
    }
    dist = 1 - dist / Math.min(norm1, norm2);
    return dist;
  }

  @Override
  public <T extends NumberVector<?>> SpatialDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
  }

  @Override
  public String toString() {
    return "HistogramIntersectionDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    return this.getClass().equals(obj.getClass());
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
    protected HistogramIntersectionDistanceFunction makeInstance() {
      return HistogramIntersectionDistanceFunction.STATIC;
    }
  }
}