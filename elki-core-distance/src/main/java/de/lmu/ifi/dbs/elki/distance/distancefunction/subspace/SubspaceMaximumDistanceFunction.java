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
package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * Maximum distance function between {@link NumberVector}s only in specified
 * dimensions.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class SubspaceMaximumDistanceFunction extends SubspaceLPNormDistanceFunction {
  /**
   * Constructor.
   * 
   * @param dimensions Selected dimensions
   */
  public SubspaceMaximumDistanceFunction(long[] dimensions) {
    super(Double.POSITIVE_INFINITY, dimensions);
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  " + "first argument: " + v1 + "\n  " + "second argument: " + v2);
    }

    double agg = 0.;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      double v = Math.abs(v1.doubleValue(d) - v2.doubleValue(d));
      agg = v > agg ? v : agg;
    }
    return agg;
  }

  @Override
  protected double minDistObject(SpatialComparable mbr, NumberVector v) {
    if(mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }

    double agg = 0.;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double value = v.doubleValue(d), diff1 = mbr.getMin(d) - value;
      if(diff1 > 0.) {
        agg = diff1 > agg ? diff1 : agg;
      }
      else {
        final double diff2 = value - mbr.getMax(d);
        agg = diff2 > agg ? diff2 : agg;
      }
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    double agg = 0.;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double max1 = mbr1.getMax(d), min2 = mbr2.getMin(d);
      if(max1 < min2) {
        final double v = min2 - max1;
        agg = v > agg ? v : agg;
      }
      else {
        final double min1 = mbr1.getMin(d), max2 = mbr2.getMax(d);
        final double v = min1 - max2;
        agg = v > agg ? v : agg;
      }
    }
    return agg;
  }

  @Override
  public double norm(NumberVector obj) {
    double agg = 0.;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      double v = Math.abs(obj.doubleValue(d));
      agg = v > agg ? v : agg;
    }
    return agg;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()) && //
        Arrays.equals(this.dimensions, ((SubspaceMaximumDistanceFunction) obj).dimensions));
  }

  @Override
  public int hashCode() {
    return this.getClass().hashCode() + BitsUtil.hashCode(dimensions);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDimensionsSelectingDistanceFunction.Parameterizer {
    @Override
    protected SubspaceMaximumDistanceFunction makeInstance() {
      return new SubspaceMaximumDistanceFunction(dimensions);
    }
  }
}
