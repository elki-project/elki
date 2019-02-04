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

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * A polynomial fit is a specific type of multiple regression. The simple
 * regression model (a first-order polynomial) can be trivially extended to
 * higher orders.
 * <p>
 * The regression model y = b0 + b1*x + b2*x^2 + ... + bp*x^p + e is a system of
 * polynomial equations of order p with polynomial coefficients { b0 ... bp}.
 * The model can be expressed using data matrix x, target double[] y and parameter
 * double[] ?. The ith row of X and Y will contain the x and y value for the ith
 * data sample.
 * <p>
 * The variables will be transformed in the following way: x =&gt; x1, ..., x^p
 * =&gt; xp Then the model can be written as a multiple linear equation model:
 * y = b0 + b1*x1 + b2*x2 + ... + bp*xp + e
 *
 * @author Elke Achtert
 * @since 0.1
 */
public class PolynomialRegression extends MultipleLinearRegression {
  /**
   * The order of the polynom.
   */
  public final int p;

  /**
   * Constructor.
   * 
   * @param y the (n x 1) - double[] holding the response values (y1, ..., yn)^T.
   * @param x the (n x 1)-double[] holding the x-values (x1, ..., xn)^T.
   * @param p the order of the polynom.
   */
  public PolynomialRegression(double[] y, double[] x, int p) {
    super(y, xMatrix(x, p));
    this.p = p;
  }

  private static double[][] xMatrix(double[] x, int p) {
    int n = x.length;

    double[][] result = new double[n][p + 1];
    for(int i = 0; i < n; i++) {
      for(int j = 0; j < p + 1; j++) {
        result[i][j] = MathUtil.powi(x[i], j);
      }
    }
    return result;
  }

  /**
   * Returns the adapted coefficient of determination
   * 
   * @return the adapted coefficient of determination
   */
  public double adaptedCoefficientOfDetermination() {
    int n = getEstimatedResiduals().length;
    return 1.0 - ((n - 1.0) / (n * 1.0 - p)) * (1 - coefficientOfDetermination());
  }

  /**
   * Performs an estimation of y on the specified x value.
   * 
   * @param x the x-value for which y is estimated
   * @return the estimation of y
   */
  public double estimateY(double x) {
    return super.estimateY(xMatrix(new double[] { x }, p));
  }
}
