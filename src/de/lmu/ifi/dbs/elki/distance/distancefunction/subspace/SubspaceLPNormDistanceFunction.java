package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DoubleNorm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a distance function that computes the Euclidean distance between
 * feature vectors only in specified dimensions.
 * 
 * @author Elke Achtert
 */
public class SubspaceLPNormDistanceFunction extends AbstractDimensionsSelectingDoubleDistanceFunction<NumberVector<?, ?>> implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>>, DoubleNorm<NumberVector<?, ?>> {
  /**
   * Value of p
   */
  private double p;

  /**
   * Constructor.
   * 
   * @param dimensions Selected dimensions
   * @param p p value
   */
  public SubspaceLPNormDistanceFunction(double p, BitSet dimensions) {
    super(dimensions);
    this.p = p;
  }

  /**
   * Get the value of p.
   * 
   * @return p
   */
  public double getP() {
    return p;
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

    double sqrDist = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      double delta = Math.abs(v1.doubleValue(d + 1) - v2.doubleValue(d + 1));
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1. / p);
  }

  protected double doubleMinDistObject(SpatialComparable mbr, NumberVector<?, ?> v) {
    if(mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }

    double sqrDist = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      final double delta;
      final double value = v.doubleValue(d + 1);
      final double omin = mbr.getMin(d + 1);
      if(value < omin) {
        delta = omin - value;
      }
      else {
        final double omax = mbr.getMax(d + 1);
        if(value > omax) {
          delta = value - omax;
        }
        else {
          continue;
        }
      }
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1. / p);
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    double sqrDist = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      final double delta;
      final double max1 = mbr1.getMax(d + 1);
      final double min2 = mbr2.getMin(d + 1);
      if(max1 < min2) {
        delta = min2 - max1;
      }
      else {
        final double min1 = mbr1.getMin(d + 1);
        final double max2 = mbr2.getMax(d + 1);
        if(min1 > max2) {
          delta = min1 - max2;
        }
        else { // The mbrs intersect!
          continue;
        }
      }
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1. / p);
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public DoubleDistance norm(NumberVector<?, ?> obj) {
    return new DoubleDistance(doubleNorm(obj));
  }

  @Override
  public double doubleNorm(NumberVector<?, ?> obj) {
    double sqrDist = 0;
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      double delta = Math.abs(obj.doubleValue(d + 1));
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1. / p);
  }

  @Override
  public <T extends NumberVector<?, ?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(database, this);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDimensionsSelectingDoubleDistanceFunction.Parameterizer {
    /**
     * Value of p
     */
    private double p;

    @Override
    protected void makeOptions(Parameterization config) {
      final DoubleParameter paramP = new DoubleParameter(LPNormDistanceFunction.P_ID, new GreaterConstraint(0));
      if(config.grab(paramP)) {
        p = paramP.getValue();
      }
      super.makeOptions(config);
    }

    @Override
    protected SubspaceLPNormDistanceFunction makeInstance() {
      if(p == 2.0) {
        return new SubspaceEuclideanDistanceFunction(dimensions);
      }
      if(p == 1.0) {
        return new SubspaceManhattanDistanceFunction(dimensions);
      }
      return new SubspaceLPNormDistanceFunction(p, dimensions);
    }
  }
}