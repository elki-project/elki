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
package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Uncentered correlation distance.
 * 
 * This is highly similar to {@link PearsonCorrelationDistanceFunction}, but
 * uses a fixed mean of 0 instead of the sample mean.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class UncenteredCorrelationDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance.
   */
  public static final UncenteredCorrelationDistanceFunction STATIC = new UncenteredCorrelationDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public UncenteredCorrelationDistanceFunction() {
    super();
  }

  /**
   * Compute the uncentered correlation of two vectors.
   * 
   * @param x first NumberVector
   * @param y second NumberVector
   * @return the uncentered correlation coefficient for x and y
   */
  public static double uncenteredCorrelation(NumberVector x, NumberVector y) {
    final int xdim = x.getDimensionality(), ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: number vectors differ in dimensionality.");
    }
    double sumXX = 0., sumYY = 0., sumXY = 0.;
    for(int i = 0; i < xdim; i++) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i);
      sumXX += xv * xv;
      sumYY += yv * yv;
      sumXY += xv * yv;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the uncentered correlation of two vectors.
   * 
   * @param x first NumberVector
   * @param y second NumberVector
   * @return the uncentered correlation coefficient for x and y
   */
  public static double uncenteredCorrelation(double[] x, double[] y) {
    final int xdim = x.length, ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: number vectors differ in dimensionality.");
    }
    double sumXX = 0., sumYY = 0., sumXY = 0.;
    for(int i = 0; i < xdim; i++) {
      final double xv = x[i], yv = y[i];
      sumXX += xv * xv;
      sumYY += yv * yv;
      sumXY += xv * yv;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Computes the Pearson correlation distance for two given feature vectors.
   * 
   * The Pearson correlation distance is computed from the Pearson correlation
   * coefficient <code>r</code> as: <code>1-r</code>. Hence, possible values of
   * this distance are between 0 and 2.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Pearson correlation distance for two given feature vectors v1
   *         and v2
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return 1. - uncenteredCorrelation(v1, v2);
  }

  @Override
  public String toString() {
    return "UncenteredCorrelationDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && this.getClass().equals(obj.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected UncenteredCorrelationDistanceFunction makeInstance() {
      return UncenteredCorrelationDistanceFunction.STATIC;
    }
  }
}
