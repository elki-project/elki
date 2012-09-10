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
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 */
public class LPNormDistanceFunction extends AbstractVectorDoubleDistanceNorm implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?>> {
  /**
   * OptionID for the "p" parameter
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

  /**
   * Keeps the currently set p.
   */
  private double p;

  /**
   * Constructor, internal version.
   * 
   * @param p Parameter p
   */
  public LPNormDistanceFunction(double p) {
    super();
    this.p = p;
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm for
   * the currently set p.
   * 
   * @param v1 first FeatureVector
   * @param v2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm for
   *         the currently set p
   */
  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }

    double sqrDist = 0;
    for(int i = 1; i <= dim1; i++) {
      final double delta = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1.0 / p);
  }

  @Override
  public double doubleNorm(NumberVector<?> v) {
    final int dim = v.getDimensionality();
    double sqrDist = 0;
    for(int i = 1; i <= dim; i++) {
      final double delta = v.doubleValue(i);
      sqrDist += Math.pow(delta, p);
    }
    return Math.pow(sqrDist, 1.0 / p);
  }

  /**
   * Get the functions p parameter.
   * 
   * @return p
   */
  public double getP() {
    return p;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Optimization for the simplest case
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?>) mbr1, (NumberVector<?>) mbr2);
      }
    }
    // TODO: optimize for more simpler cases: obj vs. rect?
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
      sumDist += Math.pow(manhattanI, p);
    }
    return Math.pow(sumDist, 1.0 / p);
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public boolean isMetric() {
    return (p >= 1);
  }

  @Override
  public String toString() {
    return "L_" + p + " Norm";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj instanceof LPNormDistanceFunction) {
      return this.p == ((LPNormDistanceFunction) obj).p;
    }
    return false;
  }

  @Override
  public <T extends NumberVector<?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The value of p.
     */
    protected double p = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter paramP = new DoubleParameter(P_ID, new GreaterConstraint(0));
      if(config.grab(paramP)) {
        p = paramP.getValue();
      }
    }

    @Override
    protected LPNormDistanceFunction makeInstance() {
      if(p == 1.0) {
        return ManhattanDistanceFunction.STATIC;
      }
      if(p == 2.0) {
        return EuclideanDistanceFunction.STATIC;
      }
      if(p == Double.POSITIVE_INFINITY) {
        return MaximumDistanceFunction.STATIC;
      }
      return new LPNormDistanceFunction(p);
    }
  }
}