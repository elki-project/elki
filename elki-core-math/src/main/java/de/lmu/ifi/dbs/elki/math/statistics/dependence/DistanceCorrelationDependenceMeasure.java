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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Distance correlation.
 * <p>
 * The value returned is the square root of the dCor² value. This matches the R
 * implementation by the original authors.
 * <p>
 * Reference:
 * <p>
 * G. J. Székely, M. L. Rizzo, N. K. Bakirov<br>
 * Measuring and testing dependence by correlation of distances<br>
 * The Annals of Statistics, 35(6), 2769-2794
 * <p>
 * Implementation notice: we exploit symmetry, and thus use diagonal matrixes.
 * While initially the diagonal is zero, after double-centering the matrix these
 * values can become non-zero!
 * 
 * @author Marie Kiermeier
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "G. J. Székely, M. L. Rizzo, N. K. Bakirov", //
    title = "Measuring and testing dependence by correlation of distances", //
    booktitle = "The Annals of Statistics, 35(6), 2769-2794", //
    url = "https://doi.org/10.1214/009053607000000505", //
    bibkey = "doi:10.1214/009053607000000505")
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
    final int len = size(adapter1, data1, adapter2, data2);
    double[] dMatrixA = computeDistances(adapter1, data1);
    double[] dMatrixB = computeDistances(adapter2, data2);

    // distance variance
    double dVarA = computeDCovar(dMatrixA, dMatrixA, len);
    if(!(dVarA > 0.)) {
      return 0.;
    }
    double dVarB = computeDCovar(dMatrixB, dMatrixB, len);
    if(!(dVarB > 0.)) {
      return 0.;
    }
    double dCovar = computeDCovar(dMatrixA, dMatrixB, len);
    // distance correlation
    return FastMath.sqrt(dCovar / FastMath.sqrt(dVarA * dVarB));
  }

  @Override
  public <A> double[] dependence(NumberArrayAdapter<?, A> adapter, List<? extends A> data) {
    final int dims = data.size();
    final int len = size(adapter, data);
    double[][] dMatrix = new double[dims][];
    for(int i = 0; i < dims; i++) {
      dMatrix[i] = computeDistances(adapter, data.get(i));
    }
    double[] dVar = new double[dims];
    for(int i = 0; i < dims; i++) {
      dVar[i] = computeDCovar(dMatrix[i], dMatrix[i], len);
    }
    double[] dCor = new double[(dims * (dims - 1)) >> 1];
    for(int y = 1, c = 0; y < dims; y++) {
      for(int x = 0; x < y; x++) {
        if(!(dVar[x] * dVar[y] > 0.)) {
          dCor[c++] = 0.;
          continue;
        }
        double dCovar = computeDCovar(dMatrix[x], dMatrix[y], len);
        dCor[c++] = FastMath.sqrt(dCovar / FastMath.sqrt(dVar[x] * dVar[y]));
      }
    }
    return dCor;
  }

  /**
   * Compute the double-centered delta matrix.
   * 
   * @param adapter Data adapter
   * @param data Input data
   * @return Double-centered delta matrix.
   */
  protected static <A> double[] computeDistances(NumberArrayAdapter<?, A> adapter, A data) {
    final int size = adapter.size(data);
    double[] dMatrix = new double[(size * (size + 1)) >> 1];
    for(int i = 0, c = 0; i < size; i++) {
      for(int j = 0; j < i; j++) {
        double dx = adapter.getDouble(data, i) - adapter.getDouble(data, j);
        dMatrix[c++] = (dx < 0) ? -dx : dx; // Absolute difference.
      }
      c++; // Diagonal entry: zero
    }
    doubleCenterMatrix(dMatrix, size);
    return dMatrix;
  }

  /**
   * Computes the distance variance matrix of one axis.
   * 
   * @param dMatrix distance matrix of the axis
   * @param size Dimensionality
   */
  public static void doubleCenterMatrix(double[] dMatrix, int size) {
    double[] rowMean = new double[size];
    // row sum
    for(int i = 0, c = 0; i < size; i++) {
      for(int j = 0; j < i; j++) {
        double v = dMatrix[c++];
        rowMean[i] += v;
        rowMean[j] += v;
      }
      assert (dMatrix[c] == 0.);
      c++; // Diagonal entry. Must be zero!
    }
    // Normalize averages:
    double matrixMean = 0.;
    for(int i = 0; i < size; i++) {
      matrixMean += rowMean[i];
      rowMean[i] /= size;
    }
    matrixMean /= size * size;

    for(int o = 0, c = 0; o < size; o++) {
      // Including row mean!
      for(int p = 0; p <= o; p++) {
        dMatrix[c++] -= rowMean[o] + rowMean[p] - matrixMean;
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
   * @return distance covariance
   */
  protected double computeDCovar(double[] dVarMatrixA, double[] dVarMatrixB, int n) {
    double result = 0.;
    for(int i = 0, c = 0; i < n; i++) {
      for(int j = 0; j < i; j++) {
        result += 2. * dVarMatrixA[c] * dVarMatrixB[c];
        c++;
      }
      // Diagonal entry.
      result += dVarMatrixA[c] * dVarMatrixB[c];
      c++;
    }
    return result / (n * n);
  }

  /**
   * Parameterization class
   * 
   * @author Marie Kiermeier
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected DistanceCorrelationDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
