package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
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
public class LPNormDistanceFunction extends AbstractVectorDoubleDistanceNorm {
  // TODO: implement SpatialPrimitiveDoubleDistanceFunction?

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
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
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
  
  //TODO: define method in interface
  /**
   * Returns the LP norm of the given vector.
   * 
   * @param v the vector to compute the norm of
   * @return the LP norm of the given vector
   */
  public double doubleNorm(NumberVector<?,?> v){
    final int dim = v.getDimensionality();
    double sqrDist = 0;
    for(int i = 1; i <= dim; i++) {
      final double delta = v.doubleValue(i);
      sqrDist += Math.pow(delta,p);
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