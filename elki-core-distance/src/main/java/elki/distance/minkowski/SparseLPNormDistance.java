/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.distance.minkowski;

import elki.data.SparseNumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.distance.Norm;
import elki.distance.PrimitiveDistance;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * L<sub>p</sub>-Norm, optimized for {@link SparseNumberVector}s.
 * <p>
 * The L<sub>p</sub> distance is defined as:
 * \[ L_p(\vec{x},\vec{y}) := \left(\sum\nolimits_i (x_i-y_i)\right)^{1/p} \]
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
// TODO: implement SpatialDistance
public class SparseLPNormDistance implements PrimitiveDistance<SparseNumberVector>, Norm<SparseNumberVector> {
  /**
   * Parameter and its inverse.
   */
  private double p, invp;

  /**
   * Constructor.
   */
  public SparseLPNormDistance(double p) {
    super();
    this.p = p;
    this.invp = 1. / p;
  }

  @Override
  public double distance(SparseNumberVector v1, SparseNumberVector v2) {
    // Get the bit masks
    double accu = 0.;
    int i1 = v1.iter(), i2 = v2.iter();
    while(v1.iterValid(i1) && v2.iterValid(i2)) {
      final int d1 = v1.iterDim(i1), d2 = v2.iterDim(i2);
      if(d1 < d2) {
        // In first only
        final double val = Math.abs(v1.iterDoubleValue(i1));
        accu += FastMath.pow(val, p);
        i1 = v1.iterAdvance(i1);
      }
      else if(d2 < d1) {
        // In second only
        final double val = Math.abs(v2.iterDoubleValue(i2));
        accu += FastMath.pow(val, p);
        i2 = v2.iterAdvance(i2);
      }
      else {
        // Both vectors have a value.
        final double val = Math.abs(v1.iterDoubleValue(i1) - v2.iterDoubleValue(i2));
        accu += FastMath.pow(val, p);
        i1 = v1.iterAdvance(i1);
        i2 = v2.iterAdvance(i2);
      }
    }
    while(v1.iterValid(i1)) {
      // In first only
      final double val = Math.abs(v1.iterDoubleValue(i1));
      accu += FastMath.pow(val, p);
      i1 = v1.iterAdvance(i1);
    }
    while(v2.iterValid(i2)) {
      // In second only
      final double val = Math.abs(v2.iterDoubleValue(i2));
      accu += FastMath.pow(val, p);
      i2 = v2.iterAdvance(i2);
    }
    return FastMath.pow(accu, invp);
  }

  @Override
  public double norm(SparseNumberVector v1) {
    double accu = 0.;
    for(int it = v1.iter(); v1.iterValid(it); it = v1.iterAdvance(it)) {
      final double val = Math.abs(v1.iterDoubleValue(it));
      accu += FastMath.pow(val, p);
    }
    return FastMath.pow(accu, invp);
  }

  @Override
  public SimpleTypeInformation<? super SparseNumberVector> getInputTypeRestriction() {
    return SparseNumberVector.VARIABLE_LENGTH;
  }

  @Override
  public boolean isMetric() {
    return (p >= 1.);
  }

  /**
   * Parameterizer
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Value for p
     */
    double p = 2.;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(LPNormDistance.Par.P_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> p = x);
    }

    @Override
    public SparseLPNormDistance make() {
      if(p == 2.) {
        return SparseEuclideanDistance.STATIC;
      }
      if(p == 1.) {
        return SparseManhattanDistance.STATIC;
      }
      if(p == Double.POSITIVE_INFINITY) {
        return SparseMaximumDistance.STATIC;
      }
      return new SparseLPNormDistance(p);
    }
  }
}
