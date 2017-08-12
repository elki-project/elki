/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * L<sub>p</sub>-Norm (Minkowski norms) are a family of distances for
 * {@link NumberVector}s.
 * 
 * The L<sub>p</sub> distance is defined as:
 * \[ L_p(\vec{x},\vec{y}) := \left(\sum_i (x_i-y_i)\right)^{1/p} \]
 * 
 * For p >= 1 this is a metric. For p=1, this yields the well known
 * {@link ManhattanDistanceFunction}, for p = 2 the standard
 * {@link EuclideanDistanceFunction}.
 * 
 * @author Arthur Zimek
 * @since 0.2
 * 
 * @apiviz.landmark
 */
@Priority(Priority.RECOMMENDED)
@Alias({ "lp", "minkowski", "p", "de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction" })
public class LPNormDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, NumberVectorDistanceFunction<NumberVector>, Norm<NumberVector> {
  /**
   * p parameter and its inverse.
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

  /**
   * Compute unscaled distance in a range of dimensions.
   * 
   * @param v1 First object
   * @param v2 Second object
   * @param start First dimension
   * @param end Exclusive last dimension
   * @return Aggregated values.
   */
  private final double preDistance(NumberVector v1, NumberVector v2, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = (xd >= yd) ? xd - yd : yd - xd;
      agg += FastMath.pow(delta, p);
    }
    return agg;
  }

  /**
   * Compute unscaled distance in a range of dimensions.
   * 
   * @param v First vector
   * @param mbr Second MBR
   * @param start First dimension
   * @param end Exclusive last dimension
   * @return Aggregated values.
   */
  private final double preDistanceVM(NumberVector v, SpatialComparable mbr, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      delta = (delta >= 0) ? delta : value - mbr.getMax(d);
      if(delta > 0.) {
        agg += FastMath.pow(delta, p);
      }
    }
    return agg;
  }

  /**
   * Compute unscaled distance in a range of dimensions.
   * 
   * @param mbr1 First MBR
   * @param mbr2 Second MBR
   * @param start First dimension
   * @param end Exclusive last dimension
   * @return Aggregated values.
   */
  private final double preDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      delta = (delta >= 0) ? delta : mbr1.getMin(d) - mbr2.getMax(d);
      if(delta > 0.) {
        agg += FastMath.pow(delta, p);
      }
    }
    return agg;
  }

  /**
   * Compute unscaled norm in a range of dimensions.
   * 
   * @param v Data object
   * @param start First dimension
   * @param end Exclusive last dimension
   * @return Aggregated values.
   */
  private final double preNorm(NumberVector v, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = xd >= 0. ? xd : -xd;
      agg += FastMath.pow(delta, p);
    }
    return agg;
  }

  /**
   * Compute unscaled norm in a range of dimensions.
   * 
   * @param mbr Data object
   * @param start First dimension
   * @param end Exclusive last dimension
   * @return Aggregated values.
   */
  private final double preNormMBR(SpatialComparable mbr, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      delta = (delta >= 0) ? delta : -mbr.getMax(d);
      if(delta > 0.) {
        agg += FastMath.pow(delta, p);
      }
    }
    return agg;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = preDistance(v1, v2, 0, mindim);
    if(dim1 > mindim) {
      agg += preNorm(v1, mindim, dim1);
    }
    else if(dim2 > mindim) {
      agg += preNorm(v2, mindim, dim2);
    }
    return FastMath.pow(agg, invp);
  }

  @Override
  public double norm(NumberVector v) {
    return FastMath.pow(preNorm(v, 0, v.getDimensionality()), invp);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality(), dim2 = mbr2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;

    final NumberVector v1 = (mbr1 instanceof NumberVector) ? (NumberVector) mbr1 : null;
    final NumberVector v2 = (mbr2 instanceof NumberVector) ? (NumberVector) mbr2 : null;

    double agg = (v1 != null) //
        ? (v2 != null) ? preDistance(v1, v2, 0, mindim) : preDistanceVM(v1, mbr2, 0, mindim) //
        : (v2 != null) ? preDistanceVM(v2, mbr1, 0, mindim) : preDistanceMBR(mbr1, mbr2, 0, mindim);
    // first object has more dimensions.
    if(dim1 > mindim) {
      agg += (v1 != null) ? preNorm(v1, mindim, dim1) : preNormMBR(mbr1, mindim, dim1);
    }
    // second object has more dimensions.
    if(dim2 > mindim) {
      agg += (v2 != null) ? preNorm(v2, mindim, dim2) : preNormMBR(mbr2, mindim, dim2);
    }
    return FastMath.pow(agg, invp);
  }

  @Override
  public boolean isMetric() {
    return (p >= 1.);
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
    return obj == this || (obj instanceof LPNormDistanceFunction //
        && this.p == ((LPNormDistanceFunction) obj).p);
  }

  @Override
  public int hashCode() {
    return Double.hashCode(p) * 31 + getClass().hashCode();
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
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
     * OptionID for the "p" parameter
     */
    public static final OptionID P_ID = new OptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

    /**
     * The value of p.
     */
    protected double p;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter paramP = new DoubleParameter(P_ID);
      paramP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(paramP)) {
        p = paramP.getValue();
      }
    }

    @Override
    protected LPNormDistanceFunction makeInstance() {
      if(p == 1.) {
        return ManhattanDistanceFunction.STATIC;
      }
      if(p == 2.) {
        return EuclideanDistanceFunction.STATIC;
      }
      if(p == Double.POSITIVE_INFINITY) {
        return MaximumDistanceFunction.STATIC;
      }
      if(p == (double) (int) p) {
        return new LPIntegerNormDistanceFunction((int) p);
      }
      return new LPNormDistanceFunction(p);
    }
  }
}
