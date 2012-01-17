package de.lmu.ifi.dbs.elki.math.statistics;

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

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * A polynomial fit is a specific type of multiple regression. The simple
 * regression model (a first-order polynomial) can be trivially extended to
 * higher orders.
 * <p/>
 * The regression model y = b0 + b1*x + b2*x^2 + ... + bp*x^p + e is a system of
 * polynomial equations of order p with polynomial coefficients { b0 ... bp}.
 * The model can be expressed using data matrix x, target vector y and parameter
 * vector ?. The ith row of X and Y will contain the x and y value for the ith
 * data sample.
 * <p/>
 * The variables will be transformed in the following way: x => x1, ..., x^p =>
 * xp Then the model can be written as a multiple linear equation model: y = b0
 * + b1*x1 + b2*x2 + ... + bp*xp + e
 * 
 * @author Elke Achtert
 */
public class PolynomialRegression extends MultipleLinearRegression {
  /**
   * The order of the polynom.
   */
  public final int p;

  /**
   * Provides a new polynomial regression model with the specified parameters.
   * 
   * @param y the (n x 1) - vector holding the response values (y1, ..., yn)^T.
   * @param x the (n x 1)-vector holding the x-values (x1, ..., xn)^T.
   * @param p the order of the polynom.
   */
  public PolynomialRegression(Vector y, Vector x, int p) {
    super(y, xMatrix(x, p));
    this.p = p;
  }

  private static Matrix xMatrix(Vector x, int p) {
    int n = x.getRowDimensionality();

    Matrix result = new Matrix(n, p + 1);
    for(int i = 0; i < n; i++) {
      for(int j = 0; j < p + 1; j++) {
        result.set(i, j, Math.pow(x.get(i), j));
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
    int n = getEstimatedResiduals().getRowDimensionality();
    return 1.0 - ((n - 1.0) / (n * 1.0 - p)) * (1 - coefficientOfDetermination());
  }

  /**
   * Performs an estimation of y on the specified x value.
   * 
   * @param x the x-value for which y is estimated
   * @return the estimation of y
   */
  public double estimateY(double x) {
    return super.estimateY(xMatrix(new Vector(new double[] { x }), p));
  }
}