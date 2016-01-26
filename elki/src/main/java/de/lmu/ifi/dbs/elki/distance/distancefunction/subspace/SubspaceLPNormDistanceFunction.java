package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * LP-Norm distance function between {@link NumberVector}s only in specified
 * dimensions.
 * 
 * @author Elke Achtert
 * @since 0.2
 */
public class SubspaceLPNormDistanceFunction extends AbstractDimensionsSelectingDistanceFunction<NumberVector> implements SpatialPrimitiveDistanceFunction<NumberVector>, Norm<NumberVector>, NumberVectorDistanceFunction<NumberVector> {
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
  public SubspaceLPNormDistanceFunction(double p, long[] dimensions) {
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

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  " + "first argument: " + v1 + "\n  " + "second argument: " + v2);
    }

    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      double delta = Math.abs(v1.doubleValue(d) - v2.doubleValue(d));
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1. / p);
  }

  protected double minDistObject(SpatialComparable mbr, NumberVector v) {
    if(mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }

    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double delta;
      final double value = v.doubleValue(d);
      final double omin = mbr.getMin(d);
      if(value < omin) {
        delta = omin - value;
      }
      else {
        final double omax = mbr.getMax(d);
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
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      final double delta;
      final double max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d);
      if(max1 < min2) {
        delta = min2 - max1;
      }
      else {
        final double min1 = mbr1.getMin(d);
        final double max2 = mbr2.getMax(d);
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
  public double norm(NumberVector obj) {
    double sqrDist = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      double delta = Math.abs(obj.doubleValue(d));
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1. / p);
  }

  @Override
  public <T extends NumberVector> SpatialPrimitiveDistanceQuery<T> instantiate(Relation<T> database) {
    return new SpatialPrimitiveDistanceQuery<>(database, this);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector> getInputTypeRestriction() {
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
  public static class Parameterizer extends AbstractDimensionsSelectingDistanceFunction.Parameterizer {
    /**
     * Value of p.
     */
    private double p;

    @Override
    protected void makeOptions(Parameterization config) {
      final DoubleParameter paramP = new DoubleParameter(LPNormDistanceFunction.Parameterizer.P_ID);
      paramP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
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
