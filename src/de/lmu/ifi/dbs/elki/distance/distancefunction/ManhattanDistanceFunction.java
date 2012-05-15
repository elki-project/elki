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
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Manhattan distance function to compute the Manhattan distance for a pair of
 * FeatureVectors.
 * 
 * @author Arthur Zimek
 */
// TODO: add spatial!
public class ManhattanDistanceFunction extends LPNormDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * The static instance to use.
   */
  public static final ManhattanDistanceFunction STATIC = new ManhattanDistanceFunction();

  /**
   * Provides a Manhattan distance function that can compute the Manhattan
   * distance (that is a DoubleDistance) for FeatureVectors.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ManhattanDistanceFunction() {
    super(1.0);
  }

  /**
   * Compute the Manhattan distance
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Manhattan distance value
   */
  @Override
  public double doubleDistance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    final int dim = v1.getDimensionality();
    if(dim != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double sum = 0;
    for(int i = 1; i <= dim; i++) {
      sum += Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
    }
    return sum;
  }
  
  /**
   * Returns the Manhattan norm of the given vector.
   * 
   * @param v the vector to compute the norm of
   * @return the Manhattan norm of the given vector
   */
  @Override
  public double doubleNorm(NumberVector<?,?> v){
    final int dim = v.getDimensionality();
    double sum = 0;
    for(int i = 1; i <= dim; i++) {
      sum += Math.abs(v.doubleValue(i));
    }
    return sum;
  }

  private double doubleMinDistObject(SpatialComparable mbr, NumberVector<?, ?> v) {
    final int dim = mbr.getDimensionality();
    if(dim != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString() + "\n" + dim + "!=" + v.getDimensionality());
    }

    double sumDist = 0;
    for(int d = 1; d <= dim; d++) {
      double value = v.doubleValue(d);
      double r;
      if(value < mbr.getMin(d)) {
        r = mbr.getMin(d);
      }
      else if(value > mbr.getMax(d)) {
        r = mbr.getMax(d);
      }
      else {
        r = value;
      }

      final double manhattanI = Math.abs(value - r);
      sumDist += manhattanI;
    }
    return sumDist;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Some optimizations for simpler cases.
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?, ?>) mbr1, (NumberVector<?, ?>) mbr2);
      }
      else {
        return doubleMinDistObject(mbr2, (NumberVector<?, ?>) mbr1);
      }
    }
    else if(mbr2 instanceof NumberVector) {
      return doubleMinDistObject(mbr1, (NumberVector<?, ?>) mbr2);
    }
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }

    double sumDist = 0;
    for(int d = 1; d <= dim1; d++) {
      final double m1, m2;
      if(mbr1.getMax(d) < mbr2.getMin(d)) {
        m1 = mbr2.getMin(d);
        m2 = mbr1.getMax(d);
      }
      else if(mbr1.getMin(d) > mbr2.getMax(d)) {
        m1 = mbr1.getMin(d);
        m2 = mbr2.getMax(d);
      }
      else { // The mbrs intersect!
        continue;
      }
      final double manhattanI = m1 - m2;
      sumDist += manhattanI;
    }
    return sumDist;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "ManhattanDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if (this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
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
    protected ManhattanDistanceFunction makeInstance() {
      return ManhattanDistanceFunction.STATIC;
    }
  }
}