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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Erich Schubert
 */
// TODO: implement SpatialDistanceFunction
public class SparseLPNormDistanceFunction extends AbstractPrimitiveDistanceFunction<SparseNumberVector<?>, DoubleDistance> implements DoubleNorm<SparseNumberVector<?>> {
  /**
   * Keeps the currently set p.
   */
  private double p;

  /**
   * Provides a LP-Norm for FeatureVectors.
   */
  public SparseLPNormDistanceFunction(double p) {
    super();
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
        double manhattanI = Math.abs(v1.doubleValue(i1) - v2.doubleValue(i2));
        sqrDist += Math.pow(manhattanI, p);
        i1 = b1.nextSetBit(i1 + 1);
        i2 = b2.nextSetBit(i2 + 1);
      }
      else if(i1 < i2 && i1 >= 0) {
        // In first only
        double manhattanI = Math.abs(v1.doubleValue(i1));
        sqrDist += Math.pow(manhattanI, p);
        i1 = b1.nextSetBit(i1 + 1);
      }
      else {
        // In second only
        double manhattanI = Math.abs(v2.doubleValue(i2));
        sqrDist += Math.pow(manhattanI, p);
        i2 = b1.nextSetBit(i2 + 1);
      }
    }
    return Math.pow(sqrDist, 1.0 / p);
  }

  @Override
  public double doubleNorm(SparseNumberVector<?> v1) {
    double sqrDist = 0;
    // Get the bit masks
    BitSet b1 = v1.getNotNullMask();
    // Set in first only
    for(int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i + 1)) {
      double manhattanI = Math.abs(v1.doubleValue(i));
      sqrDist += Math.pow(manhattanI, p);
    }
    return Math.pow(sqrDist, 1.0 / p);
  }

  @Override
  public DoubleDistance norm(SparseNumberVector<?> obj) {
    return new DoubleDistance(doubleNorm(obj));
  }

  @Override
  public DoubleDistance distance(SparseNumberVector<?> v1, SparseNumberVector<?> v2) {
    return new DoubleDistance(doubleDistance(v1, v2));
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public SimpleTypeInformation<? super SparseNumberVector<?>> getInputTypeRestriction() {
    return TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH;
  }

  @Override
  public boolean isMetric() {
    return (p >= 1);
  }

  /**
   * Parameterizer
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Value for p
     */
    double p = 2.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter pP = new DoubleParameter(LPNormDistanceFunction.P_ID, new GreaterConstraint(0));
      if(config.grab(pP)) {
        p = pP.getValue();
      }
    }

    @Override
    protected SparseLPNormDistanceFunction makeInstance() {
      if(p == 2.0) {
        return SparseEuclideanDistanceFunction.STATIC;
      }
      if(p == 1.0) {
        return SparseManhattanDistanceFunction.STATIC;
      }
      if(p == Double.POSITIVE_INFINITY) {
        return SparseMaximumDistanceFunction.STATIC;
      }
      return new SparseLPNormDistanceFunction(p);
    }
  }
}