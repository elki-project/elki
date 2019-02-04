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
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Unit test for Cholesky decomposition.
 *
 * @author Erich Schubert
 * @author Merlin Dietrich
 * @since 0.7.5
 */
public final class CholeskyDecompositionTest {
  public static double[][] TESTMATRIX_L1 = { //
      { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, }, //
      { 7, 65, 0, 0, 0, 0, 0, 0, 0, 0, }, //
      { 62, -78, 90, 0, 0, 0, 0, 0, 0, 0, }, //
      { 77, 88, -6, 35, 0, 0, 0, 0, 0, 0, }, //
      { 27, -41, -8, 30, 34, 0, 0, 0, 0, 0, }, //
      { 59, -48, 44, 4, 20, 83, 0, 0, 0, 0, }, //
      { -48, -63, -38, 35, -30, 81, 87, 0, 0, 0, }, //
      { -74, 69, 68, -65, 16, -92, -29, 87, 0, 0, }, //
      { -88, -39, 23, 81, 52, -95, 16, -90, 14, 0, }, //
      { 20, -80, -57, 14, -95, -47, -43, -26, 29, 5, } };

  public static double[][] TESTMATRIX_L2 = { //
      { 1969.5249057431356, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, //
      { -3778.931190321073, 2130.195233591261, 0, 0, 0, 0, 0, 0, 0, 0 }, //
      { 7897.304228880785, 4761.858130790322, 6641.458678884961, 0, 0, 0, 0, 0, 0, 0 }, //
      { -8224.187898172215, 650.9449507055269, -5838.595185335999, 5370.828810576402, 0, 0, 0, 0, 0, 0 }, //
      { -7478.413734477626, -6581.054183150885, -5869.786738350511, -2461.671702479478, 1729.6165087199442, 0, 0, 0, 0, 0 }, //
      { 5067.819613705262, -42.12205881216869, -2491.706436955512, -6027.931613844906, -9690.476641715957, 5532.31979037159, 0, 0, 0, 0 }, //
      { 4230.302159576424, 7047.433974545002, -3955.4819232900054, -7700.679118622675, 623.4986745110509, 6357.883612580732, 9448.074435856885, 0, 0, 0 }, //
      { -9587.321218537803, 2090.916311928586, -1463.761225539749, -2616.0553607241345, -8948.257909568407, 5722.836886508077, -9373.774396775283, 8588.838590029674, 0, 0 }, //
      { 4547.638194970525, 9171.289509833565, 1701.224402453252, 9691.325885593764, -1347.9994226315175, -4794.137302478112, 9140.72950573163, 8432.893607777965, 8708.357809689789, 0 }, //
      { 7187.132905478913, -2018.6217701736368, 2434.896101267208, 5357.489474821838, 2065.6526281452225, -4048.3345966106144, -3528.384894203782, 6580.45600445064, 5616.0250457222355, 5532.960015167152 } };

  @Test
  public void testMatrixL1() {
    CholeskyDecomposition CholL1 = new CholeskyDecomposition(timesTranspose(TESTMATRIX_L1, TESTMATRIX_L1));
    assertTrue(Arrays.deepEquals(CholL1.getL(), TESTMATRIX_L1));
    assertTrue(VMath.equals(CholL1.getL(), TESTMATRIX_L1));
  }

  @Test
  public void testMatrixL2() {
    CholeskyDecomposition CholL2 = new CholeskyDecomposition(timesTranspose(TESTMATRIX_L2, TESTMATRIX_L2));
    assertTrue(almostEquals(CholL2.getL(), TESTMATRIX_L2, 1e-9));
  }

  /**
   * Testing the isSPD (is symmetric positive definite) method of
   * CholeskyDecomposition class.
   */
  @Test
  public void testIsSPD() {
    assertTrue(new CholeskyDecomposition(timesTranspose(TESTMATRIX_L1, TESTMATRIX_L1)).isSPD());
    assertTrue(new CholeskyDecomposition(timesTranspose(TESTMATRIX_L2, TESTMATRIX_L2)).isSPD());

    // symmetric but not positive definite Matrix
    final double[][] A3 = { //
        { -1, 3, 2 }, //
        { 3, -12, 2 }, //
        { 2, 2, 5 } };
    assertFalse(new CholeskyDecomposition(A3).isSPD());

    // non symmetric positive definite Matrix
    assertFalse(new CholeskyDecomposition(new double[][] { { 1, 3 }, { 2, 12 } }).isSPD());
  }

  @Test
  public void testJamaSolve() { // Test case from Jama 1.0.2
    double[][] p = { { 4., 1., 1. }, { 1., 2., 3. }, { 1., 3., 6. } };
    CholeskyDecomposition c = new CholeskyDecomposition(p);
    double[][] l = c.getL();
    assertTrue(almostEquals(p, timesTranspose(l, l), 1e-15));
    double[][] o = c.solve(unitMatrix(3));
    assertTrue(almostEquals(unitMatrix(3), times(p, o), 1e-14));
  }

  /**
   * Testing the Solve method of the CholeskyDecomposition class.
   */
  @Test
  public void testSolve() {
    final double[] a = { 3, 7, 7, 7, -7, 7, 7, 7, 7, 7 };
    final double[] b = { -2, 3, 5, 1, 8, 1, 1, 1, 1, 1 };
    final double[][] m = transpose(new double[][] { a, b });

    final double[][] A1 = timesTranspose(TESTMATRIX_L1, TESTMATRIX_L1);
    final double[][] A2 = timesTranspose(TESTMATRIX_L2, TESTMATRIX_L2);

    final double[] a1 = new CholeskyDecomposition(A1).solve(a);
    final double[] a2 = new CholeskyDecomposition(A2).solve(a);
    final double[][] a1t = new CholeskyDecomposition(A1).solve(transpose(transpose(a)));
    final double[][] a2t = new CholeskyDecomposition(A2).solve(transpose(transpose(a)));
    final double[] b1 = new CholeskyDecomposition(A1).solve(b);
    final double[] b2 = new CholeskyDecomposition(A2).solve(b);
    final double[][] b1t = new CholeskyDecomposition(A1).solve(transpose(transpose(b)));
    final double[][] b2t = new CholeskyDecomposition(A2).solve(transpose(transpose(b)));
    final double[][] m1 = new CholeskyDecomposition(A1).solve(m);
    final double[][] m2 = new CholeskyDecomposition(A2).solve(m);

    assertTrue(almostEquals(times(A1, a1), a));
    assertTrue(almostEquals(times(A2, a2), a));
    assertTrue(almostEquals(times(A1, a1t), transpose(transpose(a))));
    assertTrue(almostEquals(times(A2, a2t), transpose(transpose(a))));
    assertTrue(almostEquals(times(A1, b1), b));
    assertTrue(almostEquals(times(A2, b2), b));
    assertTrue(almostEquals(times(A1, b1t), transpose(transpose(b))));
    assertTrue(almostEquals(times(A2, b2t), transpose(transpose(b))));
    assertTrue(almostEquals(times(A1, m1), m));
    assertTrue(almostEquals(times(A2, m2), m));
  }

  /**
   * Testing that the solve method of the CholeskyDecomposition class raises an
   * exception if the row dimensions do not agree.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSolveIsNonSingular() {
    new CholeskyDecomposition(new double[][] { { 1, 1 }, { 1, 1 } }).solve(new double[][] { {} });
  }

  /**
   * Testing that the solve method of the CholeskyDecomposition class raises an
   * exception if isSPD returns false, so if the matrix is not symmetric and
   * positive definite.
   */
  @Test(expected = ArithmeticException.class)
  public void testSolveRowDimensionMismatch() {
    new CholeskyDecomposition(new double[][] { { 1, -13 }, { 2, 12 } }).solve(new double[][] { { 1 }, { 1 } });
  }
}
