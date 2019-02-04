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
package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * L<sub>p</sub>-Norm for {@link NumberVector}s, optimized version for integer
 * values of p. This will likely not have huge impact, but may vary from CPU and
 * virtual machine version.
 * <p>
 * When using the parameterization API, {@link LPNormDistanceFunction} will
 * automatically use this class for integer values.
 * <p>
 * The L<sub>p</sub> distance is defined as:
 * \[ L_p(\vec{x},\vec{y}) := \left(\sum_i (x_i-y_i)\right)^{1/p} \]
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @opt nodefillcolor LemonChiffon
 */
public class LPIntegerNormDistanceFunction extends LPNormDistanceFunction {
  /**
   * Integer value of p.
   */
  int intp;

  /**
   * Constructor, internal version.
   * 
   * @param p Parameter p
   */
  public LPIntegerNormDistanceFunction(int p) {
    super(p);
    this.intp = p;
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
  private double preDistance(NumberVector v1, NumberVector v2, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = xd >= yd ? xd - yd : yd - xd;
      agg += MathUtil.powi(delta, intp);
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
  private double preDistanceVM(NumberVector v, SpatialComparable mbr, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      delta = delta >= 0 ? delta : value - mbr.getMax(d);
      if(delta > 0.) {
        agg += MathUtil.powi(delta, intp);
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
  private double preDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      delta = delta >= 0 ? delta : mbr1.getMin(d) - mbr2.getMax(d);
      if(delta > 0.) {
        agg += MathUtil.powi(delta, intp);
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
  private double preNorm(NumberVector v, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = xd >= 0. ? xd : -xd;
      agg += MathUtil.powi(delta, intp);
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
  private double preNormMBR(SpatialComparable mbr, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      delta = delta >= 0 ? delta : -mbr.getMax(d);
      if(delta > 0.) {
        agg += MathUtil.powi(delta, intp);
      }
    }
    return agg;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;
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
    final int mindim = dim1 < dim2 ? dim1 : dim2;

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
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()) //
        && this.intp == ((LPIntegerNormDistanceFunction) obj).intp);
  }

  @Override
  public int hashCode() {
    return intp * 31 + getClass().hashCode();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The value of p.
     */
    protected int p;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter paramP = new IntParameter(LPNormDistanceFunction.Parameterizer.P_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(paramP)) {
        p = paramP.getValue();
      }
    }

    @Override
    protected LPIntegerNormDistanceFunction makeInstance() {
      return p == 1 ? ManhattanDistanceFunction.STATIC : //
          p == 2 ? EuclideanDistanceFunction.STATIC : // F
              new LPIntegerNormDistanceFunction(p);
    }
  }
}
