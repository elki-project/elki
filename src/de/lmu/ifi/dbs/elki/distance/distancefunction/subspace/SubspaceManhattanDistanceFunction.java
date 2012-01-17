package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Provides a distance function that computes the Euclidean distance between
 * feature vectors only in specified dimensions.
 * 
 * @author Elke Achtert
 */
public class SubspaceManhattanDistanceFunction extends SubspaceLPNormDistanceFunction {
  /**
   * Constructor.
   * 
   * @param dimensions Selected dimensions
   */
  public SubspaceManhattanDistanceFunction(BitSet dimensions) {
    super(1.0, dimensions);
  }

  /**
   * Provides the Euclidean distance between two given feature vectors in the
   * selected dimensions.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Euclidean distance between two given feature vectors in the
   *         selected dimensions
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  " + "first argument: " + v1 + "\n  " + "second argument: " + v2);
    }

    double sum = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      sum += Math.abs(v1.doubleValue(d + 1) - v2.doubleValue(d + 1));
    }
    return sum;
  }

  protected double doubleMinDistObject(SpatialComparable mbr, NumberVector<?, ?> v) {
    if(mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }

    double sum = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      final double value = v.doubleValue(d + 1);
      final double omin = mbr.getMin(d + 1);
      if(value < omin) {
        sum += omin - value;
      }
      else {
        final double omax = mbr.getMax(d + 1);
        if(value > omax) {
          sum += value - omax;
        }
        else {
          continue;
        }
      }
    }
    return sum;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    double sum = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      final double max1 = mbr1.getMax(d + 1);
      final double min2 = mbr2.getMin(d + 1);
      if(max1 < min2) {
        sum += min2 - max1;
      }
      else {
        final double min1 = mbr1.getMin(d + 1);
        final double max2 = mbr2.getMax(d + 1);
        if(min1 > max2) {
          sum += min1 - max2;
        }
        else { // The mbrs intersect!
          continue;
        }
      }
    }
    return sum;
  }

  @Override
  public double doubleNorm(NumberVector<?, ?> obj) {
    double sum = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      sum += Math.abs(obj.doubleValue(d + 1));
    }
    return sum;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDimensionsSelectingDoubleDistanceFunction.Parameterizer {
    @Override
    protected SubspaceManhattanDistanceFunction makeInstance() {
      return new SubspaceManhattanDistanceFunction(dimensions);
    }
  }
}