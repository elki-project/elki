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
package de.lmu.ifi.dbs.elki.math.statistics;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.inverse;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minus;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesTranspose;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimes;

import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Multiple linear regression attempts to model the relationship between two or
 * more explanatory variables and a response variable by fitting a linear
 * equation to observed data. Every value of the independent variable x is
 * associated with a value of the dependent variable y.
 * <p>
 * The population regression line for p explanatory variables x1, x2, ... , xp
 * is defined to be y = b0 + b1*x1 + b2*x2 + ... + bp*xp + e.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class MultipleLinearRegression {
  /**
   * The (n x 1) - double[] holding the y-values (y1, ..., yn)^T.
   */
  private final double[] y;

  /**
   * Holds the mean value of the y-values.
   */
  private final double y_mean;

  /**
   * The (n x p+1)-matrix holding the x-values, where the i-th row has the form
   * (1 x1i ... x1p).
   */
  private final double[][] x;

  /**
   * The (p+1 x 1) - double[] holding the estimated b-values (b0, b1, ...,
   * bp)^T.
   */
  private final double[] b;

  /**
   * The (n x 1) - double[] holding the estimated residuals (e1, ..., en)^T.
   */
  private final double[] e;

  /**
   * The error variance.
   */
  private final double variance;

  /**
   * Holds the matrix (x'x)^-1.
   */
  private final double[][] xx_inverse;

  /**
   * The sum of square residuals
   */
  private final double ssr;

  /**
   * The sum of square totals
   */
  private final double sst;

  /**
   * Constructor.
   * 
   * @param y the (n x 1) - double[] holding the response values (y1, ...,
   *        yn)^T.
   * @param x the (n x p+1)-matrix holding the explanatory values, where the
   *        i-th row has the form (1 x1i ... x1p).
   */
  public MultipleLinearRegression(double[] y, double[][] x) {
    if(y.length <= x[0].length) {
      throw new IllegalArgumentException("Number of observed data has to be greater than " + "number of regressors: " + y.length + " > " + x[0].length);
    }

    this.y = y;
    this.x = x;

    double sum = 0;
    for(int i = 0; i < y.length; i++) {
      sum += y[i];
    }
    y_mean = sum / y.length;

    // estimate b, e
    xx_inverse = inverse(transposeTimes(x, x));
    b = times(timesTranspose(xx_inverse, x), y);
    // b = new double[](x.solve(y).getColumnPackedCopy());
    e = minus(y, times(x, b));

    // sum of square residuals: ssr
    sum = 0;
    for(int i = 0; i < e.length; i++) {
      sum += e[i] * e[i];
    }
    ssr = sum;

    // sum of square totals: sst
    sum = 0;
    for(int i = 0; i < y.length; i++) {
      final double diff = y[i] - y_mean;
      sum += diff * diff;
    }
    sst = sum;

    // variance
    variance = ssr / (y.length - x[0].length - 1);
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    StringBuilder msg = new StringBuilder(1000);
    FormatUtil.formatTo(msg.append("x = "), x, "[", "]\n", ", ", FormatUtil.NF8);
    FormatUtil.formatTo(msg.append("\ny = "), y, ", ", FormatUtil.NF8);
    FormatUtil.formatTo(msg.append("\nb = "), b, ", ", FormatUtil.NF8);
    FormatUtil.formatTo(msg.append("\ne = "), e, ", ", FormatUtil.NF8);
    return msg.append("error variance = ").append(FormatUtil.NF4.format(variance)).toString();
  }

  /**
   * Returns the sum of squares total.
   * 
   * @return the sum of squares total
   */
  public double getSumOfSquaresTotal() {
    return sst;
  }

  /**
   * Returns the sum of square residuals.
   * 
   * @return the sum of square residuals
   */
  public double getSumOfSquareResiduals() {
    return ssr;
  }

  /**
   * Returns the estimated coefficients
   * 
   * @return the estimated coefficients
   */
  public double[] getEstimatedCoefficients() {
    return b;
  }

  /**
   * Returns the estimated residuals
   * 
   * @return the estimated residuals
   */
  public double[] getEstimatedResiduals() {
    return e;
  }

  /**
   * Returns the coefficient of determination
   * 
   * @return the coefficient of determination
   */
  public double coefficientOfDetermination() {
    return 1.0 - (ssr / sst);
  }

  /**
   * Perform an estimation of y on the specified matrix.
   * 
   * @param x the matrix for which y is estimated
   * @return the estimation of y
   */
  public double estimateY(double[][] x) {
    return times(x, b)[0];
  }

  /**
   * Returns the error variance.
   * 
   * @return the error variance
   */
  public double getVariance() {
    return variance;
  }
}
