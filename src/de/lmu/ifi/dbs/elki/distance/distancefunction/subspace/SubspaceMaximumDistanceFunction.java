package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
public class SubspaceMaximumDistanceFunction extends SubspaceLPNormDistanceFunction {
  /**
   * Constructor.
   * 
   * @param dimensions Selected dimensions
   */
  public SubspaceMaximumDistanceFunction(BitSet dimensions) {
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
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    if (v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  " + "first argument: " + v1 + "\n  " + "second argument: " + v2);
    }

    double agg = 0.;
    for (int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      double v = Math.abs(v1.doubleValue(d) - v2.doubleValue(d));
      if (v > agg) {
        agg = v;
      }
    }
    return agg;
  }

  @Override
  protected double doubleMinDistObject(SpatialComparable mbr, NumberVector<?> v) {
    if (mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }

    double agg = 0.;
    for (int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      final double value = v.doubleValue(d);
      final double omin = mbr.getMin(d);
      final double diff1 = omin - value;
      if (diff1 > 0.) {
        if (diff1 > agg) {
          agg = diff1;
        }
      } else {
        final double omax = mbr.getMax(d);
        final double diff2 = value - omax;
        if (diff2 > agg) {
          agg = diff2;
        }
      }
    }
    return agg;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    double agg = 0.;
    for (int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      final double max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d);
      if (max1 < min2) {
        double v = min2 - max1;
        if (v > agg) {
          agg = v;
        }
      } else {
        final double min1 = mbr1.getMin(d);
        final double max2 = mbr2.getMax(d);
        double v = min1 - max2;
        if (v > agg) {
          agg = v;
        }
      }
    }
    return agg;
  }

  @Override
  public double doubleNorm(NumberVector<?> obj) {
    double agg = 0.;
    for (int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      double v = Math.abs(obj.doubleValue(d));
      if (v > agg) {
        agg = v;
      }
    }
    return agg;
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
    protected SubspaceMaximumDistanceFunction makeInstance() {
      return new SubspaceMaximumDistanceFunction(dimensions);
    }
  }
}
