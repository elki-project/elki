package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialDoubleDistanceNorm;
import de.lmu.ifi.dbs.elki.utilities.Alias;
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
@Alias({ "lp", "minkowski", "p", "de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction" })
public class LPNormDistanceFunction extends AbstractSpatialDoubleDistanceNorm {
  /**
   * OptionID for the "p" parameter
   */
  public static final OptionID P_ID = new OptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

  /**
   * Keeps the currently set p and its inverse
   */
  protected double p, invp;

  /**
   * Constructor, internal version.
   * 
   * @param p Parameter p
   */
  public LPNormDistanceFunction(double p) {
    super();
    this.p = p;
    this.invp = 1. / p;
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = (xd >= yd) ? xd - yd : yd - xd;
      agg += Math.pow(delta, p);
    }
    return Math.pow(agg, invp);
  }

  @Override
  public double doubleNorm(NumberVector<?> v) {
    final int dim = v.getDimensionality();
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double xd = v.doubleValue(d);
      agg += Math.pow(xd >= 0. ? xd : -xd, p);
    }
    return Math.pow(agg, invp);
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Optimization for the simplest case
    if (mbr1 instanceof NumberVector) {
      if (mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?>) mbr1, (NumberVector<?>) mbr2);
      }
    }
    // TODO: optimize for more simpler cases: obj vs. rect?
    final int dim = dimensionality(mbr1, mbr2);

    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double diff;
      final double d1 = mbr2.getMin(d) - mbr1.getMax(d);
      if (d1 > 0.) {
        diff = d1;
      } else {
        final double d2 = mbr1.getMin(d) - mbr2.getMax(d);
        if (d2 > 0.) {
          diff = d2;
        } else { // The mbrs intersect!
          continue;
        }
      }
      agg += Math.pow(diff, p);
    }
    return Math.pow(agg, invp);
  }

  @Override
  public boolean isMetric() {
    return (p >= 1);
  }

  @Override
  public String toString() {
    return "L_" + p + "Norm";
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
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof LPNormDistanceFunction) {
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
    protected double p;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter paramP = new DoubleParameter(P_ID);
      paramP.addConstraint(new GreaterConstraint(0.));
      if (config.grab(paramP)) {
        p = paramP.getValue();
      }
    }

    @Override
    protected LPNormDistanceFunction makeInstance() {
      if (p == 1.0) {
        return ManhattanDistanceFunction.STATIC;
      }
      if (p == 2.0) {
        return EuclideanDistanceFunction.STATIC;
      }
      if (p == Double.POSITIVE_INFINITY) {
        return MaximumDistanceFunction.STATIC;
      }
      return new LPNormDistanceFunction(p);
    }
  }
}
