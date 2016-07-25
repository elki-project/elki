package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * LP-Norm for {@link NumberVector}s, optimized version for integer values of p.
 * This will likely not have huge impact, but may vary from CPU and virtual
 * machine version.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.landmark
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

  private final double preDistance(NumberVector v1, NumberVector v2, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = (xd >= yd) ? xd - yd : yd - xd;
      agg += MathUtil.powi(delta, intp);
    }
    return agg;
  }

  private final double preDistanceVM(NumberVector v, SpatialComparable mbr, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      if(delta < 0.) {
        delta = value - mbr.getMax(d);
      }
      if(delta > 0.) {
        agg += MathUtil.powi(delta, intp);
      }
    }
    return agg;
  }

  private final double preDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      if(delta < 0.) {
        delta = mbr1.getMin(d) - mbr2.getMax(d);
      }
      if(delta > 0.) {
        agg += MathUtil.powi(delta, intp);
      }
    }
    return agg;
  }

  private final double preNorm(NumberVector v, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = xd >= 0. ? xd : -xd;
      agg += MathUtil.powi(delta, intp);
    }
    return agg;
  }

  private final double preNormMBR(SpatialComparable mbr, final int start, final int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      if(delta < 0.) {
        delta = -mbr.getMax(d);
      }
      if(delta > 0.) {
        agg += MathUtil.powi(delta, intp);
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
    return Math.pow(agg, invp);
  }

  @Override
  public double norm(NumberVector v) {
    return Math.pow(preNorm(v, 0, v.getDimensionality()), invp);
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
    return Math.pow(agg, invp);
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
    protected int p;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter paramP = new IntParameter(LPNormDistanceFunction.Parameterizer.P_ID);
      paramP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(paramP)) {
        p = paramP.getValue();
      }
    }

    @Override
    protected LPIntegerNormDistanceFunction makeInstance() {
      if(p == 1) {
        return ManhattanDistanceFunction.STATIC;
      }
      if(p == 2) {
        return EuclideanDistanceFunction.STATIC;
      }
      return new LPIntegerNormDistanceFunction(p);
    }
  }
}
