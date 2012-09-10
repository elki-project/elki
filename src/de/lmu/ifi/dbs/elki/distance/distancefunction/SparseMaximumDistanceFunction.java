package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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

/**
 * Maximum distance function. Optimized for sparse vectors.
 * 
 * @author Erich Schubert
 */
public class SparseMaximumDistanceFunction extends SparseLPNormDistanceFunction {
  /**
   * Static instance
   */
  public static final SparseMaximumDistanceFunction STATIC = new SparseMaximumDistanceFunction();
  
  /**
   * Constructor.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public SparseMaximumDistanceFunction() {
    super(Double.POSITIVE_INFINITY);
  }

  @Override
  public double doubleDistance(SparseNumberVector<?> v1, SparseNumberVector<?> v2) {
    // Get the bit masks
    BitSet b1 = v1.getNotNullMask();
    BitSet b2 = v2.getNotNullMask();
    double sqrDist = 0;
    int i1 = b1.nextSetBit(0);
    int i2 = b2.nextSetBit(0);
    while(i1 >= 0 && i2 >= 0) {
      if(i1 == i2) {
        // Set in both
        sqrDist = Math.max(sqrDist, Math.abs(v1.doubleValue(i1) - v2.doubleValue(i2)));
        i1 = b1.nextSetBit(i1 + 1);
        i2 = b2.nextSetBit(i2 + 1);
      }
      else if(i1 < i2 && i1 >= 0) {
        // In first only
        sqrDist = Math.max(sqrDist, Math.abs(v1.doubleValue(i1)));
        i1 = b1.nextSetBit(i1 + 1);
      }
      else {
        // In second only
        double manhattanI = Math.abs(v2.doubleValue(i2));
        sqrDist = Math.max(sqrDist, manhattanI);
        i2 = b1.nextSetBit(i2 + 1);
      }
    }
    return sqrDist;
  }

  @Override
  public double doubleNorm(SparseNumberVector<?> v1) {
    double sqrDist = 0;
    // Get the bit masks
    BitSet b1 = v1.getNotNullMask();
    // Set in first only
    for(int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i + 1)) {
      sqrDist = Math.max(sqrDist, Math.abs(v1.doubleValue(i)));
    }
    return sqrDist;
  }

  /**
   * Parameterizer
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SparseMaximumDistanceFunction makeInstance() {
      return SparseMaximumDistanceFunction.STATIC;
    }
  }
}