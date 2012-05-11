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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Weighted version of the Euclidean distance function.
 * 
 * @author Erich Schubert
 */
// TODO: make parameterizable; add optimized variants
public class WeightedLPNormDistanceFunction extends LPNormDistanceFunction {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   * 
   * @param p p value
   * @param weights Weight vector
   */
  public WeightedLPNormDistanceFunction(double p, double[] weights) {
    super(p);
    this.weights = weights;
  }

  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim = weights.length;
    if(dim != v1.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality of FeatureVector doesn't match weights!");
    }
    if(dim != v2.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality of FeatureVector doesn't match weights!");
    }

    final double p = getP();
    double sqrDist = 0;
    for(int i = 1; i <= dim; i++) {
      final double delta = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      sqrDist += Math.pow(delta, p) * weights[i - 1];
    }
    return Math.pow(sqrDist, 1.0 / p);
  }
  
  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Optimization for the simplest case
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?, ?>) mbr1, (NumberVector<?, ?>) mbr2);
      }
    }
    // TODO: optimize for more simpler cases: obj vs. rect?
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }

    final double p = getP();
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
      sumDist += Math.pow(manhattanI, p) * weights[d - 1];
    }
    return Math.pow(sumDist, 1.0 / p);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof WeightedLPNormDistanceFunction)) {
      if(obj instanceof LPNormDistanceFunction && super.equals(obj)) {
        for(double d : weights) {
          if(d != 1.0) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
    WeightedLPNormDistanceFunction other = (WeightedLPNormDistanceFunction) obj;
    return Arrays.equals(this.weights, other.weights);
  }
}