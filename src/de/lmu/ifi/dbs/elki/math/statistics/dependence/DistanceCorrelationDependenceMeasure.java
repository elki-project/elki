package de.lmu.ifi.dbs.elki.math.statistics.dependence;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance correlation.
 * 
 * Reference:
 * <p>
 * Székely, G. J., Rizzo, M. L., & Bakirov, N. K.<br />
 * Measuring and testing dependence by correlation of distances<br />
 * The Annals of Statistics, 35(6), 2769-2794
 * </p>
 * 
 * @author Marie Kiermeier
 * @author Erich Schubert
 */
@Reference(authors = "Székely, G. J., Rizzo, M. L., & Bakirov, N. K.", //
title = "Measuring and testing dependence by correlation of distances", //
booktitle = "The Annals of Statistics, 35(6), 2769-2794", //
url = "http://dx.doi.org/10.1214/009053607000000505")
public class DistanceCorrelationDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final DistanceCorrelationDependenceMeasure STATIC = new DistanceCorrelationDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance instead!
   */
  protected DistanceCorrelationDependenceMeasure() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int size = size(adapter1, data1, adapter2, data2);
    double[][] dMatrixA = computeDistances(adapter1, data1);
    double[][] dMatrixB = computeDistances(adapter2, data2);

    // distance variance
    double dVarA = computeDCovar(dMatrixA, dMatrixA, size);
    double dVarB = computeDCovar(dMatrixB, dMatrixB, size);
    double dCovar = computeDCovar(dMatrixA, dMatrixB, size);
    // distance correlation
    return (dVarA * dVarB > 0) ? Math.sqrt(dCovar / Math.sqrt(dVarA * dVarB)) : 0.;
  }

  /**
   * Compute the double-centered delta matrix.
   * 
   * @param adapter Data adapter
   * @param data Input data
   * @return Double-centered delta matrix.
   */
  protected static <A> double[][] computeDistances(NumberArrayAdapter<?, A> adapter, A data) {
    final int size = adapter.size(data);
    double[][] dMatrixB = new double[size][size];
    for(int i = 0; i < size; i++) {
      for(int j = 0; j < i; j++) {
        double dx = adapter.getDouble(data, i) - adapter.getDouble(data, j);
        dx = (dx < 0) ? -dx : dx; // Absolute difference.
        dMatrixB[i][j] = dx;
        dMatrixB[j][i] = dx; // Symmetry.
      }
    }
    doubleCenterMatrix(dMatrixB);
    return dMatrixB;
  }

  /**
   * Computes the distance variance matrix of one axis.
   * 
   * @param dMatrix distance matrix of the axis
   */
  public static void doubleCenterMatrix(double[][] dMatrix) {
    final int size = dMatrix.length;
    double[] rowMean = new double[size];
    double[] colMean = new double[size];
    double matrixMean = 0.;
    // row sum
    for(int i = 0; i < size; i++) {
      for(int j = 0; j < size; j++) {
        double v = dMatrix[i][j];
        rowMean[i] += v;
        colMean[j] += v;
      }
      matrixMean += rowMean[i];
    }
    // Normalize averages:
    for(int i = 0; i < size; i++) {
      rowMean[i] /= size;
      colMean[i] /= size;
    }
    matrixMean /= size * size;

    for(int o = 0; o < size; o++) {
      for(int p = 0; p < size; p++) {
        dMatrix[o][p] = dMatrix[o][p] - rowMean[o] - colMean[p] + matrixMean;
      }
    }
  }

  /**
   * Computes the distance covariance for two axis. Can also be used to compute
   * the distance variance of one axis (dVarMatrixA = dVarMatrixB).
   * 
   * @param dVarMatrixA distance variance matrix of the first axis
   * @param dVarMatrixB distance variance matrix of the second axis
   * @param n number of points
   * @return
   */
  protected double computeDCovar(double[][] dVarMatrixA, double[][] dVarMatrixB, int n) {
    double result = 0.;
    // TODO: exploit symmetry?
    for(int i = 0; i < n; i++) {
      for(int j = 0; j < n; j++) {
        result += dVarMatrixA[i][j] * dVarMatrixB[i][j];
      }
    }
    return result / (n * n);
  }

  /**
   * 
   * Parameterization class
   * 
   * @author Marie Kiermeier
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected DistanceCorrelationDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
