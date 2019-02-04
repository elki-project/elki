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
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import net.jafama.FastMath;

/**
 * Euclidean distance function between {@link NumberVector}s only in specified
 * dimensions.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
@Alias("de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction")
public class SubspaceEuclideanDistanceFunction extends SubspaceLPNormDistanceFunction {
  /**
   * Constructor.
   * 
   * @param dimensions Selected dimensions
   */
  public SubspaceEuclideanDistanceFunction(long[] dimensions) {
    super(2.0, dimensions);
  }

  /**
   * Constructor.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Euclidean distance between two given feature vectors in the
   *         selected dimensions
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  " + "first argument: " + v1 + "\n  " + "second argument: " + v2);
    }

    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double delta = v1.doubleValue(d) - v2.doubleValue(d);
      sqrDist += delta * delta;
    }
    return FastMath.sqrt(sqrDist);
  }

  @Override
  protected double minDistObject(SpatialComparable mbr, NumberVector v) {
    if(mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }

    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double value = v.doubleValue(d), omin = mbr.getMin(d);
      if(value < omin) {
        final double delta = omin - value;
        sqrDist += delta * delta;
      }
      else {
        final double omax = mbr.getMax(d);
        if(value > omax) {
          final double delta = value - omax;
          sqrDist += delta * delta;
        }
        // Else they intersect.
      }
    }
    return FastMath.sqrt(sqrDist);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double max1 = mbr1.getMax(d), min2 = mbr2.getMin(d);
      if(max1 < min2) {
        final double delta = min2 - max1;
        sqrDist += delta * delta;
      }
      else {
        final double min1 = mbr1.getMin(d), max2 = mbr2.getMax(d);
        if(min1 > max2) {
          final double delta = min1 - max2;
          sqrDist += delta * delta;
        }
        // Else the mbrs intersect!
      }
    }
    return FastMath.sqrt(sqrDist);
  }

  @Override
  public double norm(NumberVector obj) {
    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double delta = obj.doubleValue(d);
      sqrDist += delta * delta;
    }
    return FastMath.sqrt(sqrDist);
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()) && //
        Arrays.equals(this.dimensions, ((SubspaceEuclideanDistanceFunction) obj).dimensions));
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
    protected SubspaceEuclideanDistanceFunction makeInstance() {
      return new SubspaceEuclideanDistanceFunction(dimensions);
    }
  }
}
