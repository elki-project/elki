package de.lmu.ifi.dbs.elki.distance.distancefunction;

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
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Cosine distance function for feature vectors.
 * 
 * The cosine distance is computed as the arcus from the cosine similarity
 * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
 * 
 * @author Arthur Zimek
 */
public class ArcCosineDistanceFunction extends AbstractVectorDoubleDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * Static instance
   */
  public static final ArcCosineDistanceFunction STATIC = new ArcCosineDistanceFunction();

  /**
   * Provides a CosineDistanceFunction.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ArcCosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed as the arcus from the cosine similarity
   * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    double d = Math.acos(VectorUtil.angle(v1, v2));
    if(d < 0) {
      d = 0;
    }
    return d;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Essentially, we want to compute this:
    // max(v1.transposeTimes(v2)) / (min(v1.euclideanLength()) *
    // min(v2.euclideanLength()));
    // We can just compute all three in parallel.
    final int dim = mbr1.getDimensionality();
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < dim; k++) {
      s += mbr1.getMax(k + 1) * mbr2.getMax(k + 1);
      final double r1 = mbr1.getMin(k + 1);
      final double r2 = mbr2.getMin(k + 1);
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    double d = Math.acos(Math.sqrt((s / e1) * (s / e2)));
    if(d < 0) {
      d = 0;
    }
    return d;
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public String toString() {
    return "ArcCosineDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    return this.getClass().equals(obj.getClass());
  }

  @Override
  public <T extends NumberVector<?, ?>> SpatialDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
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
    protected ArcCosineDistanceFunction makeInstance() {
      return ArcCosineDistanceFunction.STATIC;
    }
  }
}