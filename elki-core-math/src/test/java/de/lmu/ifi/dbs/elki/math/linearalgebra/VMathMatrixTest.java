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
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMathOperationsTest.assertDimensionMismatch;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Class testing the methods of the {@link VMath} class,
 * which provide mathematical operations and have two dimensional double arrays
 * as input.
 * <p>
 * Two dimensional double arrays are interpreted as rows x columns matrixes.
 *
 * @author Merlin Dietrich
 * @since 0.7.5
 */
@SuppressWarnings("deprecation")
public class VMathMatrixTest {
  /**
   * A non symmetric 4 x 5 (rows x columns) matrix as test-data.
   */
  protected static final double[][] TESTMATRIX = { //
      { 1, 2, 3, 4, 5 }, //
      { 2, 3, 4, 5, 1 }, //
      { 3, 4, 5, 1, 2 }, //
      { 4, 5, 1, 2, 3 } };

  /**
   * Testing the transposed method of VMath class.
   */
  @Test
  public void testTransposed() {
    final double[][] m1 = TESTMATRIX;
    final double[][] res_case1 = { //
        { m1[0][0], m1[1][0], m1[2][0], m1[3][0] }, //
        { m1[0][1], m1[1][1], m1[2][1], m1[3][1] }, //
        { m1[0][2], m1[1][2], m1[2][2], m1[3][2] }, //
        { m1[0][3], m1[1][3], m1[2][3], m1[3][3] }, //
        { m1[0][4], m1[1][4], m1[2][4], m1[3][4] } };
    assertTrue(almostEquals(transpose(m1), res_case1));
  }

  /**
   * Testing the plus, plusEquals methods of VMath class.
   */
  @Test
  public void testPlus() {
    final double[][] m1 = { //
        { -14, -1, 0.01, 0.100000000000006100004 }, //
        { 1.21, 0, 2, 0.500000000000006100004 }, //
        { -14, 1, -3.42, 1.100000000000006100007 } };
    final double[][] m2 = { //
        { 7, 6, 0, -0.1000000000000069 }, //
        { -1.21, -9, 2, 0.4000000000000069 }, //
        { -3, 26, 1, 0 } };
    final double s = 13;
    final double[][] res_plus = { //
        { m1[0][0] + m2[0][0], m1[0][1] + m2[0][1], m1[0][2] + m2[0][2], m1[0][3] + m2[0][3] }, //
        { m1[1][0] + m2[1][0], m1[1][1] + m2[1][1], m1[1][2] + m2[1][2], m1[1][3] + m2[1][3] }, //
        { m1[2][0] + m2[2][0], m1[2][1] + m2[2][1], m1[2][2] + m2[2][2], m1[2][3] + m2[2][3] } };
    final double[][] res_plusTimes = { { m1[0][0] + s * m2[0][0], m1[0][1] + s * m2[0][1], m1[0][2] + s * m2[0][2], m1[0][3] + s * m2[0][3] }, //
        { m1[1][0] + s * m2[1][0], m1[1][1] + s * m2[1][1], m1[1][2] + s * m2[1][2], m1[1][3] + s * m2[1][3] }, //
        { m1[2][0] + s * m2[2][0], m1[2][1] + s * m2[2][1], m1[2][2] + s * m2[2][2], m1[2][3] + s * m2[2][3] } };

    // testing plus and plusEquals
    assertTrue(almostEquals(res_plus, plus(m1, m2), 0.));
    assertTrue(almostEquals(res_plus, plusEquals(copy(m1), m2), 0.));
    // via minusTimes
    assertTrue(almostEquals(res_plus, plusTimes(m1, m2, 1), 0.));

    // testing plusTimes and plusTimesEquals
    assertTrue(almostEquals(res_plusTimes, plusTimes(m1, m2, s), 0.));
    assertTrue(almostEquals(res_plusTimes, plusTimesEquals(copy(m1), m2, s), 0.));
  }

  /**
   * Testing the matrix minus, minusEquals methods of VMath class.
   */
  @Test
  public void testMinus() {
    final double[][] m1 = { //
        { 13, 0, 5 }, //
        { -3, 1, 3 }, //
        { 1, 2, -4.7 }, //
        { -7, -2.21, 72 }, //
        { -27, 12, 0 } };
    final double[][] m2 = { //
        { -13, 0.1, 0.0000000000000006 }, //
        { 5, 42, 2 }, //
        { 7, 3, 4.7 }, //
        { -7, 4, 2 }, //
        { 7, 6, -6 } };
    final double s = 13;
    final double[][] res_minus = { //
        { m1[0][0] - m2[0][0], m1[0][1] - m2[0][1], m1[0][2] - m2[0][2] }, //
        { m1[1][0] - m2[1][0], m1[1][1] - m2[1][1], m1[1][2] - m2[1][2] }, //
        { m1[2][0] - m2[2][0], m1[2][1] - m2[2][1], m1[2][2] - m2[2][2] }, //
        { m1[3][0] - m2[3][0], m1[3][1] - m2[3][1], m1[3][2] - m2[3][2] }, //
        { m1[4][0] - m2[4][0], m1[4][1] - m2[4][1], m1[4][2] - m2[4][2] } };
    final double[][] res_minusTimes = { //
        { m1[0][0] - s * m2[0][0], m1[0][1] - s * m2[0][1], m1[0][2] - s * m2[0][2] }, //
        { m1[1][0] - s * m2[1][0], m1[1][1] - s * m2[1][1], m1[1][2] - s * m2[1][2] }, //
        { m1[2][0] - s * m2[2][0], m1[2][1] - s * m2[2][1], m1[2][2] - s * m2[2][2] }, //
        { m1[3][0] - s * m2[3][0], m1[3][1] - s * m2[3][1], m1[3][2] - s * m2[3][2] }, //
        { m1[4][0] - s * m2[4][0], m1[4][1] - s * m2[4][1], m1[4][2] - s * m2[4][2] } };

    // testing minus and minusEquals (Matrix - Matrix)
    assertTrue(almostEquals(res_minus, minus(m1, m2), 0.));
    assertTrue(almostEquals(res_minus, minusEquals(copy(m1), m2), 0.));
    // via minusTimes
    assertTrue(almostEquals(res_minus, minusTimes(m1, m2, 1), 0.));

    // testing minusTimes and minusTimesEquals
    assertTrue(almostEquals(res_minusTimes, minusTimes(m1, m2, s), 0.));
    assertTrue(almostEquals(res_minusTimes, minusTimesEquals(copy(m1), m2, s), 0.));
    // via plusTimes (this is a test for plusTimes as well)
    assertTrue(almostEquals(plusTimes(m1, m2, -s), minusTimes(m1, m2, s), 0.));
  }

  /**
   * Testing the matrix times scalar multiplication methods times, timesEquals
   * of VMath class.
   */
  @Test
  public void testMatrixScalarMultiplication() {
    final double[][] m1 = TESTMATRIX;
    final double s1 = 1. / 3;
    final double[][] m1_times_one_third = { //
        { s1 * m1[0][0], s1 * m1[0][1], s1 * m1[0][2], s1 * m1[0][3], s1 * m1[0][4] }, //
        { s1 * m1[1][0], s1 * m1[1][1], s1 * m1[1][2], s1 * m1[1][3], s1 * m1[1][4] }, //
        { s1 * m1[2][0], s1 * m1[2][1], s1 * m1[2][2], s1 * m1[2][3], s1 * m1[2][4] }, //
        { s1 * m1[3][0], s1 * m1[3][1], s1 * m1[3][2], s1 * m1[3][3], s1 * m1[3][4] } };

    assertTrue(almostEquals(m1_times_one_third, times(m1, s1), 0.));
    assertTrue(almostEquals(m1_times_one_third, timesEquals(m1, s1), 0.));

    // check if 0 works a clear
    final double[][] m1_times_zero = new double[m1.length][m1[0].length];

    assertTrue(almostEquals(times(m1, 0), m1_times_zero));
    assertTrue(almostEquals(timesEquals(m1, 0), m1_times_zero));

    // check if 1 works a identity
    assertTrue(almostEquals(times(m1, 1), m1));
    assertTrue(almostEquals(timesEquals(m1, 1), m1));
  }

  /**
   * Testing the matrix times matrix multiplications methods of VMath class.
   *
   * The following VMath methods are tested:<br>
   * times, transposeTimesTranspose, timesTranspose, transposeTimes
   */
  @Test
  public void testMatrixMatrixMultiplication() {
    // testing times and transposedTimestranspose
    final double[][] m1 = { //
        { 1.21, 2000 }, //
        { 0, -1 }, //
        { 7, 0 } };
    final double[][] m2 = { //
        { -1.21, 2000, 2 }, //
        { -700, -2368, 4.3 } };
    final double[][] res_times = { //
        { m1[0][0] * m2[0][0] + m1[0][1] * m2[1][0], m1[0][0] * m2[0][1] + m1[0][1] * m2[1][1], m1[0][0] * m2[0][2] + m1[0][1] * m2[1][2] }, //
        { m1[1][0] * m2[0][0] + m1[1][1] * m2[1][0], m1[1][0] * m2[0][1] + m1[1][1] * m2[1][1], m1[1][0] * m2[0][2] + m1[1][1] * m2[1][2] }, //
        { m1[2][0] * m2[0][0] + m1[2][1] * m2[1][0], m1[2][0] * m2[0][1] + m1[2][1] * m2[1][1], m1[2][0] * m2[0][2] + m1[2][1] * m2[1][2] } };
    final double[][] res_transTimesTrans = { //
        { m1[0][0] * m2[0][0] + m1[1][0] * m2[0][1] + m1[2][0] * m2[0][2], m1[0][0] * m2[1][0] + m1[1][0] * m2[1][1] + m1[2][0] * m2[1][2] }, //
        { m1[0][1] * m2[0][0] + m1[1][1] * m2[0][1] + m1[2][1] * m2[0][2], m1[0][1] * m2[1][0] + m1[1][1] * m2[1][1] + m1[2][1] * m2[1][2] } };

    assertTrue(almostEquals(times(m1, m2), res_times));
    assertTrue(almostEquals(transposeTimesTranspose(m2, m1), transpose(res_times)));

    assertTrue(almostEquals(transposeTimesTranspose(m1, m2), res_transTimesTrans));
    assertTrue(almostEquals(times(m2, m1), transpose(res_transTimesTrans)));

    // general testing and testing transposeTimes and timesTranspose
    final double[][] m3 = TESTMATRIX;
    final double[][] m4 = { //
        { 5, 0, 4, 3, 1 }, //
        { 9, -3, 2, 8, 8 }, //
        { -4, -1, 4, 9, -9 }, //
        { 1, 1, 7, 5, 7 } };
    final double[][] m3_t = transpose(m3), m4_t = transpose(m4);

    // check times(Matrix, id) = Matrix = times(id, Matrix)
    assertTrue(almostEquals(times(m3, unitMatrix(5)), m3));
    assertTrue(almostEquals(times(unitMatrix(4), m3), m3));

    // check transposeTimesTranspose(Matrix, id) = transpose(Matrix) =
    // transposeTimesTranspose(id, Matrix)
    assertTrue(almostEquals(transposeTimesTranspose(m3, unitMatrix(4)), m3_t));
    assertTrue(almostEquals(transposeTimesTranspose(unitMatrix(5), m3), m3_t));

    final double[][] m3_times_m4transposed = { //
        { transposeTimes(m3[0], m4[0]), transposeTimes(m3[0], m4[1]), transposeTimes(m3[0], m4[2]), transposeTimes(m3[0], m4[3]) }, //
        { transposeTimes(m3[1], m4[0]), transposeTimes(m3[1], m4[1]), transposeTimes(m3[1], m4[2]), transposeTimes(m3[1], m4[3]) }, //
        { transposeTimes(m3[2], m4[0]), transposeTimes(m3[2], m4[1]), transposeTimes(m3[2], m4[2]), transposeTimes(m3[2], m4[3]) }, //
        { transposeTimes(m3[3], m4[0]), transposeTimes(m3[3], m4[1]), transposeTimes(m3[3], m4[2]), transposeTimes(m3[3], m4[3]) } };

    // testing timesTranspose without not using a matrix methods times
    assertTrue(almostEquals(timesTranspose(m3, m4), m3_times_m4transposed));

    // testing timesTranspose without not using a vector method transposeTimes
    // this is at the same time a test for the times method assuming the test
    // before succeeded.
    assertTrue(almostEquals(timesTranspose(m3, m4), times(m3, m4_t)));
    // and the following analog a test for the transposeTimesTranspose method
    assertTrue(almostEquals(timesTranspose(m3, m4), transposeTimesTranspose(m3_t, m4)));

    final double[][] m3transposed_times_m4 = { //
        { transposeTimes(m3_t[0], m4_t[0]), transposeTimes(m3_t[0], m4_t[1]), transposeTimes(m3_t[0], m4_t[2]), transposeTimes(m3_t[0], m4_t[3]), transposeTimes(m3_t[0], m4_t[4]) }, //
        { transposeTimes(m3_t[1], m4_t[0]), transposeTimes(m3_t[1], m4_t[1]), transposeTimes(m3_t[1], m4_t[2]), transposeTimes(m3_t[1], m4_t[3]), transposeTimes(m3_t[1], m4_t[4]) }, //
        { transposeTimes(m3_t[2], m4_t[0]), transposeTimes(m3_t[2], m4_t[1]), transposeTimes(m3_t[2], m4_t[2]), transposeTimes(m3_t[2], m4_t[3]), transposeTimes(m3_t[2], m4_t[4]) }, //
        { transposeTimes(m3_t[3], m4_t[0]), transposeTimes(m3_t[3], m4_t[1]), transposeTimes(m3_t[3], m4_t[2]), transposeTimes(m3_t[3], m4_t[3]), transposeTimes(m3_t[3], m4_t[4]) }, //
        { transposeTimes(m3_t[4], m4_t[0]), transposeTimes(m3_t[4], m4_t[1]), transposeTimes(m3_t[4], m4_t[2]), transposeTimes(m3_t[4], m4_t[3]), transposeTimes(m3_t[4], m4_t[4]) } };

    // testing transposeTimes without not using a matrix methods times
    // without transpose and times
    assertTrue(almostEquals(transposeTimes(m3, m4), m3transposed_times_m4));

    // testing transposeTimes without using a vector method timesTransposed
    // this is as well a test for the times method assuming the test before
    // succeeded.
    assertTrue(almostEquals(transposeTimes(m3, m4), times(m3_t, m4)));
    // and the following analog a test for the transposeTimesTranspose method
    assertTrue(almostEquals(transposeTimes(m3, m4), transposeTimesTranspose(m3, m4_t)));
  }

  /**
   * Testing the Matrix times Vector multiplications methods of VMath class.
   *
   * The following VMath methods are tested:<br>
   * times(matrix, vector), times(vector, matrix), transposeTimes(matrix,
   * vector), transposeTimes(vector, matrix),
   * timesTranspose(vector, matrix), transposeTimesTimes(vector, matrix, vector)
   */
  @Test
  public void testMatrixVectorMultiplication() {
    final double[] v1 = { 1.21, 2 }, v2 = { 3, 0.5, -3 };
    final double[][] m1 = { //
        { 1.21, 2000 }, //
        { 0, -1 }, //
        { 7, 0 } };
    final double[] res_times = { m1[0][0] * v1[0] + m1[0][1] * v1[1], m1[1][0] * v1[0] + m1[1][1] * v1[1], m1[2][0] * v1[0] + m1[2][1] * v1[1], };
    final double[] res_transposeTimes = { m1[0][0] * v2[0] + m1[1][0] * v2[1] + m1[2][0] * v2[2], m1[0][1] * v2[0] + m1[1][1] * v2[1] + m1[2][1] * v2[2] };
    final double res_transposeTimesTimes = (m1[0][0] * v2[0] + m1[1][0] * v2[1] + m1[2][0] * v2[2]) * v1[0] + (m1[0][1] * v2[0] + m1[1][1] * v2[1] + m1[2][1] * v2[2]) * v1[1];

    // testing times(m1, v2)
    assertArrayEquals(res_times, times(m1, v1), 0.);
    assertArrayEquals(transpose(m1)[0], times(m1, unitVector(2, 0)), 0.);
    assertArrayEquals(transpose(m1)[1], times(m1, unitVector(2, 1)), 0.);

    // testing transposeTimes(m1, v2);
    assertArrayEquals(res_transposeTimes, transposeTimes(m1, v2), 0.);
    assertArrayEquals(m1[0], transposeTimes(m1, unitVector(3, 0)), 0.);
    assertArrayEquals(m1[1], transposeTimes(m1, unitVector(3, 1)), 0.);
    assertArrayEquals(m1[2], transposeTimes(m1, unitVector(3, 2)), 0.);

    // testing transposeTimesTimes(a, B, c);
    assertEquals(res_transposeTimesTimes, transposeTimesTimes(v2, m1, v1), 0.);
    assertEquals(transposeTimes(v2, times(m1, v1)), transposeTimesTimes(v2, m1, v1), 0.);
    assertEquals(times(transposeTimes(v2, m1), v1)[0], transposeTimesTimes(v2, m1, v1), 0.);

    // testing transposedTimes(vector, Matrix) via
    // transpose(transposeTimes(matrix, vector))
    assertTrue(VMath.equals(transposeTimes(unitVector(3, 0), m1), transpose(transposeTimes(m1, unitVector(3, 0)))));
    assertTrue(VMath.equals(transposeTimes(unitVector(3, 1), m1), transpose(transposeTimes(m1, unitVector(3, 1)))));
    assertTrue(VMath.equals(transposeTimes(unitVector(3, 2), m1), transpose(transposeTimes(m1, unitVector(3, 2)))));
  }

  /**
   * Testing the timesTranspose(vector, matrix), times(vector, matrix)
   * multiplication methods of VMath class,
   * where the matrix as two dimensional double array actually need to be a
   * vector.
   */
  @Test
  public void testVectorVectorMultiplication() {
    final double[] v3 = { 1, 2, -7 };
    final double[] v4 = { -5, 1, 3, -4 };
    final double[][] m2 = { v4 };
    final double[][] m2_t = { { v4[0] }, { v4[1] }, { v4[2] }, { v4[3] } };
    final double[][] res = { //
        { v3[0] * v4[0], v3[0] * v4[1], v3[0] * v4[2], v3[0] * v4[3] }, //
        { v3[1] * v4[0], v3[1] * v4[1], v3[1] * v4[2], v3[1] * v4[3] }, //
        { v3[2] * v4[0], v3[2] * v4[1], v3[2] * v4[2], v3[2] * v4[3] } };

    // testing times(vector, matrix)
    assertTrue(almostEquals(times(v3, m2), res));
    // via timesTranspose(vector,vector)
    assertTrue(almostEquals(times(v3, m2), timesTranspose(v3, v4)));

    // testing timesTranspose(vector, matrix)
    assertTrue(almostEquals(timesTranspose(v3, m2_t), res));
    // via timesTranspose(vector,vector)
    assertTrue(almostEquals(timesTranspose(v3, m2_t), timesTranspose(v3, v4)));
  }

  /**
   * Testing the unitMatrix and the identity method of VMath class.
   */
  @Test
  public void testUnitMatrix() {
    // test unitMatrix(dim) and unitMatrix(dim) equals identity(dim, dim)
    final double[][] m_unit = { { 1, 0, 0, 0, 0 }, { 0, 1, 0, 0, 0 }, { 0, 0, 1, 0, 0 }, { 0, 0, 0, 1, 0 }, { 0, 0, 0, 0, 1 } };
    assertTrue(VMath.equals(unitMatrix(5), m_unit));
    assertTrue(VMath.equals(identity(5, 5), m_unit));

    // test identity with dimensions 3x5 and 5x3
    final double[][] m_identity3x5 = { { 1, 0, 0, 0, 0 }, { 0, 1, 0, 0, 0 }, { 0, 0, 1, 0, 0 } };
    assertTrue(VMath.equals(identity(3, 5), m_identity3x5));
    assertTrue(VMath.equals(identity(5, 3), transpose(m_identity3x5)));
  }

  @Test
  public void testFrobenius() {
    final double[][] m = identity(4, 6);
    assertEquals(2, normF(m), 0.);
    m[0][5] = 5;
    assertEquals(Math.sqrt(29), normF(m), 0.);
  }

  /**
   * Testing that the *Equals methods tested in this class
   * class work in place and that other methods tested
   * create a new instance.
   * 
   * Methods where the class of the instance
   * returned differs form class of the input are reasonably omitted
   * when testing reference.
   */
  @Test
  public void testReference() {
    final double[][] m1 = { { 42 } };
    final double[] inner = { 42 };
    final double[][] m2 = { inner };

    // Ensure the compiler did not optimize away this test.
    assertNotSame(m1[0], m2[0]);

    // testing the methods as in testInverse
    assertNotSame(m1, inverse(m1));

    // testing the methods as in testMatrixMatrixMultiplication
    assertNotSame(m1, times(m1, m1));

    assertNotSame(m1, transposeTimesTranspose(m1, m1));

    assertNotSame(m1, timesTranspose(m1, m1));

    assertNotSame(m1, transposeTimes(m1, m1));

    // testing the methods as in testMatrixScalarMultiplication
    final double s1 = 3.14;
    assertNotSame(m1, times(m1, s1));
    assertSame(m1, timesEquals(m1, s1));

    // testing the methods as in testTranspose
    assertNotSame(m1, transpose(m1));

    // testing the methods as in testMatrixVectorMultiplication
    final double[] v1 = { 3.14 };
    assertNotSame(m1, times(v1, m1));
    assertNotSame(m1, times(m1, v1));

    assertNotSame(m1, transposeTimes(m1, v1));
    assertNotSame(m1, transposeTimes(v1, m1));

    assertNotSame(m1, timesTranspose(v1, m1));

    // testing the methods as in testMinus
    assertNotSame(m1, minus(m1, m2));
    assertSame(m1, minusEquals(m1, m2));

    assertNotSame(m1, minusTimes(m1, m1, s1));
    assertSame(m1, minusTimesEquals(m1, m1, s1));

    // testing the methods as in testPlus
    assertNotSame(m1, plus(m1, m2));
    assertSame(m1, plusEquals(m1, m2));

    assertNotSame(m1, plusTimes(m1, m1, s1));
    assertSame(m1, plusTimesEquals(m1, m1, s1));

    // testing the methods as in testTraspose
    assertNotSame(m1, transpose(m1));
  }

  /**
   * Testing that correct error is raised when dimension of the input data
   * mismatch the needs of the method.
   * Methods where no error is to be raised are omitted.
   * <p>
   * See {@link VMathOperationsTest#assertDimensionMismatch} for details.
   */
  @Test
  public void testDimensionMismatch() {
    // testing the appendColums method
    assertDimensionMismatch("m.getRowDimension() != column.getRowDimension()", () -> appendColumns(identity(3, 2), identity(2, 2)));

    // testing the methods as in testMatrixMatrixMultiplication
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> times(unitMatrix(3), identity(2, 3)));
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> transposeTimesTranspose(unitMatrix(3), identity(3, 2)));
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> timesTranspose(unitMatrix(3), identity(3, 2)));
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> transposeTimes(unitMatrix(3), identity(2, 3)));

    // testing the methods as in testMatrixVectorMultiplication
    // vector length and number of rows in matrix differ
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> times(identity(2, 3), unitVector(2, 0)));

    // vector length and number of columns in matrix differ
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> transposeTimes(identity(2, 3), unitVector(3, 0)));

    // first vector has wrong length
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> transposeTimesTimes(unitVector(3, 0), identity(2, 3), unitVector(3, 0)));
    // second vector has wrong length
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> transposeTimesTimes(unitVector(2, 0), identity(2, 3), unitVector(2, 0)));

    // matrix has more than one row
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> times(unitVector(3, 0), identity(2, 3)));

    // matrix has more than one column
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> timesTranspose(unitVector(3, 0), identity(3, 2)));

    // vector length and number of rows in matrix differ
    assertDimensionMismatch(ERR_MATRIX_INNERDIM, () -> transposeTimes(unitVector(3, 0), identity(2, 3)));

    // testing the methods as in testMinus
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> minus(identity(3, 3), identity(2, 3)));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> minusEquals(identity(2, 2), identity(2, 3)));

    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> minusTimes(identity(3, 3), identity(2, 3), 1));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> minusTimesEquals(identity(2, 2), identity(2, 3), 1));

    // testing the methods as in testPlus
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> plus(identity(3, 3), identity(2, 3)));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> plusEquals(identity(2, 2), identity(2, 3)));

    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> plusTimes(identity(3, 3), identity(2, 3), 1));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> plusTimesEquals(identity(2, 2), identity(2, 3), 1));

    // testSolve, testTraspose, testUnitMatrix are tested in separate classes
  }
}
