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

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Class testing the methods of the {@link VMath} class, which provide
 * mathematical operations and have
 * one dimensional double arrays as input.
 *
 * One dimensional double arrays are interpreted column vectors.
 *
 * @author Merlin Dietrich
 * @since 0.7.5
 */
public final class VMathVectorTest {
  /**
   * A vector of length 5 as test-data.
   */
  protected static final double[] TESTVEC = { 2, 3, 5, 7, 9 };

  /**
   * Testing the vector times scalar method of VMath class.
   */
  @Test
  public void testTimes() {
    final double[] v1 = { 1, 3, 9, 7 };
    final double s_case1 = 5;
    final double s_case2 = 1. / 3;
    final double s_case3 = 0.100000000000006100004;

    final double[] res_case1 = { v1[0] * s_case1, v1[1] * s_case1, v1[2] * s_case1, v1[3] * s_case1 };
    final double[] res_case2 = { v1[0] * s_case2, v1[1] * s_case2, v1[2] * s_case2, v1[3] * s_case2 };
    final double[] res_case3 = { v1[0] * s_case3, v1[1] * s_case3, v1[2] * s_case3, v1[3] * s_case3 };

    // test the three cases
    assertArrayEquals(res_case1, times(v1, s_case1), 0.);
    assertArrayEquals(res_case2, times(v1, s_case2), 0.);
    assertArrayEquals(res_case3, times(v1, s_case3), 0.);
  }

  /**
   * Testing VMath methods, which operate as addition on vectors.
   * <p>
   * The following VMath methods are tested:<br>
   * plus,plusEquals; timesPlus, timesPlusEquals;
   * plusTimes, plusTimesEquals, timesPlus, timesPlusEquals;
   * timesPlustimes, timesPlustimesEquals
   */
  @Test
  public void testPlus() {
    /*
     * test-data I:
     * Four dimensional test vectors, Near 0 numerical loss of precision in ResultData res* and
     * mixed positive and negative numbers.
     */
    final double[] v1_I = { -14, 1, 2, 0.100000000000006100004 };
    final double[] v2_I = { 7, 6, 2, -0.1000000000000069 };
    final double s_I = 13, s1_I = 2, s2_I = -3;

    final double[] res_plus_I = { v1_I[0] + v2_I[0], v1_I[1] + v2_I[1], v1_I[2] + v2_I[2], v1_I[3] + v2_I[3] };
    final double[] res_plus_scal_I = { v1_I[0] + s_I, v1_I[1] + s_I, v1_I[2] + s_I, v1_I[3] + s_I };
    final double[] res_timesPlus_I = { s1_I * v1_I[0] + v2_I[0], s1_I * v1_I[1] + v2_I[1], s1_I * v1_I[2] + v2_I[2], s1_I * v1_I[3] + v2_I[3] };
    final double[] res_plusTimes_I = { v1_I[0] + s2_I * v2_I[0], v1_I[1] + s2_I * v2_I[1], v1_I[2] + s2_I * v2_I[2], v1_I[3] + s2_I * v2_I[3] };
    final double[] res_timesPlustimes_I = { s1_I * v1_I[0] + s2_I * v2_I[0], s1_I * v1_I[1] + s2_I * v2_I[1], s1_I * v1_I[2] + s2_I * v2_I[2], s1_I * v1_I[3] + s2_I * v2_I[3] };

    // testing plus and plusEquals
    assertArrayEquals(res_plus_I, plus(v1_I, v2_I), 0.);
    assertArrayEquals(res_plus_I, plusEquals(copy(v1_I), v2_I), 0.);

    // testing plus and plusEquals
    assertArrayEquals(res_plus_scal_I, plus(v1_I, s_I), 0.);
    assertArrayEquals(res_plus_scal_I, plusEquals(copy(v1_I), s_I), 0.);

    // testing timesPlus and timesPlusEquals
    assertArrayEquals(res_timesPlus_I, timesPlus(v1_I, s1_I, v2_I), 0.);
    assertArrayEquals(res_timesPlus_I, timesPlusEquals(copy(v1_I), s1_I, v2_I), 0.);

    // testing plusTimes and plusTimesEquals
    assertArrayEquals(res_plusTimes_I, plusTimes(v1_I, v2_I, s2_I), 0.);
    assertArrayEquals(res_plusTimes_I, plusTimesEquals(copy(v1_I), v2_I, s2_I), 0.);

    // testing timesPlustimes and timesPlustimesEquals
    assertArrayEquals(res_timesPlustimes_I, timesPlusTimes(v1_I, s1_I, v2_I, s2_I), 0.);
    assertArrayEquals(res_timesPlustimes_I, timesPlusTimesEquals(copy(v1_I), s1_I, v2_I, s2_I), 0);

    /*
     * test-data II:
     * Three dimensional test vectors of type double with 5 decimal places as
     * mantissa. Numbers are strictly positive.
     */
    final double delta = 1E-10;
    final double[] v1_II = { 0.17825, 32.546, 2958.3 };
    final double[] v2_II = { 0.82175, 67.454, 7041.7 };

    final double s_II = 0.92175, s1_II = 1.5, s2_II = 0.5;

    final double[] res_plus_II = { v1_II[0] + v2_II[0], v1_II[1] + v2_II[1], v1_II[2] + v2_II[2] };
    final double[] res_plus_scal_II = { v1_II[0] + s_II, v1_II[1] + s_II, v1_II[2] + s_II };
    final double[] res_timesPlus_II = { s1_II * v1_II[0] + v2_II[0], s1_II * v1_II[1] + v2_II[1], s1_II * v1_II[2] + v2_II[2] };
    final double[] res_plusTimes_II = { v1_II[0] + s2_II * v2_II[0], v1_II[1] + s2_II * v2_II[1], v1_II[2] + s2_II * v2_II[2] };
    final double[] res_timesPlustimes_II = { s1_II * v1_II[0] + s2_II * v2_II[0], s1_II * v1_II[1] + s2_II * v2_II[1], s1_II * v1_II[2] + s2_II * v2_II[2] };

    // testing plus and plusEquals (Vector + Vector)
    assertArrayEquals(res_plus_II, plus(v1_II, v2_II), delta);
    assertArrayEquals(res_plus_II, plusEquals(copy(v1_II), v2_II), delta);

    // testingplus() and plusEquals (Vector + Skalar)
    assertArrayEquals(res_plus_scal_II, plus(v1_II, s_II), delta);
    assertArrayEquals(res_plus_scal_II, plusEquals(copy(v1_II), s_II), delta);

    // testing timesPlus() and timesPlusEquals()
    assertArrayEquals(res_timesPlus_II, timesPlus(v1_II, s1_II, v2_II), delta);
    assertArrayEquals(res_timesPlus_II, timesPlusEquals(copy(v1_II), s1_II, v2_II), delta);

    // testing plusTimes() and plusTimesEquals()
    assertArrayEquals(res_plusTimes_II, plusTimes(v1_II, v2_II, s2_II), delta);
    assertArrayEquals(res_plusTimes_II, plusTimesEquals(copy(v1_II), v2_II, s2_II), delta);

    // testing timesPlustimes() and timesPlustimesEquals()
    assertArrayEquals(res_timesPlustimes_II, timesPlusTimes(v1_II, s1_II, v2_II, s2_II), delta);
    assertArrayEquals(res_timesPlustimes_II, timesPlusTimesEquals(copy(v1_II), s1_II, v2_II, s2_II), delta);

    /*
     * general testing
     */
    final double[] v5 = { 1, 2, 3 }, v6 = { 4, 5, 6 };
    final double s5 = 2, s6 = 3;

    // consistency check to minus method
    assertArrayEquals(minus(v5, times(v6, -1)), plus(v5, v6), 0.);
    // consistency check within the plus methods
    assertArrayEquals(plus(v5, times(v6, s6)), plusTimes(v5, v6, s6), 0.);
    assertArrayEquals(plus(times(v5, s5), v6), timesPlus(v5, s5, v6), 0.);
    assertArrayEquals(plus(times(v5, s5), times(v6, s6)), timesPlusTimes(v5, s5, v6, s6), 0.);
  }

  /**
   * Testing VMath methods, which operate as subtraction on vectors.
   * <p>
   * The following VMath methods are tested:<br>
   * minus, minusEquals, timesMinus, timesMinusEquals;
   * minusTimes, minusTimesEquals, timesMinus, timesMinusEquals;
   * timesMinustimes, timesMinustimesEquals
   */
  @Test
  public void testMinus() {
    // test case I
    final double[] v1 = { -14, 1, 2, 0.100000000000006100004 };
    final double[] v2 = { 7, 6, 2, -0.1000000000000069 };

    final double s0 = 13, s1 = 2, s2 = -3;
    final double[] res_minus_I = { v1[0] - v2[0], v1[1] - v2[1], v1[2] - v2[2], v1[3] - v2[3] };
    final double[] res_minus_scal_I = { v1[0] - s0, v1[1] - s0, v1[2] - s0, v1[3] - s0 };
    final double[] res_timesMinus_I = { s1 * v1[0] - v2[0], s1 * v1[1] - v2[1], s1 * v1[2] - v2[2], s1 * v1[3] - v2[3] };
    final double[] res_minusTimes_I = { v1[0] - s2 * v2[0], v1[1] - s2 * v2[1], v1[2] - s2 * v2[2], v1[3] - s2 * v2[3] };
    final double[] res_timesMinustimes_I = { s1 * v1[0] - s2 * v2[0], s1 * v1[1] - s2 * v2[1], s1 * v1[2] - s2 * v2[2], s1 * v1[3] - s2 * v2[3] };

    // minus and minusEquals
    assertArrayEquals(res_minus_I, minus(v1, v2), 0.);
    assertArrayEquals(res_minus_I, minusEquals(copy(v1), v2), 0.);

    // minus and minusEquals
    assertArrayEquals(res_minus_scal_I, minus(v1, s0), 0.);
    assertArrayEquals(res_minus_scal_I, minusEquals(copy(v1), s0), 0.);

    // timesMinus and timesMinusEquals
    assertArrayEquals(res_timesMinus_I, timesMinus(v1, s1, v2), 0.);
    assertArrayEquals(res_timesMinus_I, timesMinusEquals(copy(v1), s1, v2), 0.);

    // minusTimes and minusTimesEquals
    assertArrayEquals(res_minusTimes_I, minusTimes(v1, v2, s2), 0.);
    assertArrayEquals(res_minusTimes_I, minusTimesEquals(copy(v1), v2, s2), 0.);

    // timesMinustimes and timesMinustimesEquals
    assertArrayEquals(res_timesMinustimes_I, timesMinusTimes(v1, s1, v2, s2), 0.);
    assertArrayEquals(res_timesMinustimes_I, timesMinusTimesEquals(copy(v1), s1, v2, s2), 0);

    // test case II
    final double delta = 1E-10;
    final double[] v3 = { 0.17825, 32.546, 2958.3 };
    final double[] v4 = { 0.82175, 67.454, 7041.7 };

    final double s00 = 0.92175, s3 = 1.5, s4 = 0.5;

    final double[] res_minus_II = { v3[0] - v4[0], v3[1] - v4[1], v3[2] - v4[2] };
    final double[] res_minus_scal_II = { v3[0] - s00, v3[1] - s00, v3[2] - s00 };
    final double[] res_timesMinus_II = { s3 * v3[0] - v4[0], s3 * v3[1] - v4[1], s3 * v3[2] - v4[2] };
    final double[] res_minusTimes_II = { v3[0] - s4 * v4[0], v3[1] - s4 * v4[1], v3[2] - s4 * v4[2] };
    final double[] res_timesMinustimes_II = { s3 * v3[0] - s4 * v4[0], s3 * v3[1] - s4 * v4[1], s3 * v3[2] - s4 * v4[2] };

    // minus and minusEquals
    assertArrayEquals(res_minus_II, minus(v3, v4), delta);
    assertArrayEquals(res_minus_II, minusEquals(copy(v3), v4), delta);

    // minus and minusEquals
    assertArrayEquals(res_minus_scal_II, minus(v3, s00), delta);
    assertArrayEquals(res_minus_scal_II, minusEquals(copy(v3), s00), delta);

    // timesMinus and timesMinusEquals
    assertArrayEquals(res_timesMinus_II, timesMinus(v3, s3, v4), delta);
    assertArrayEquals(res_timesMinus_II, timesMinusEquals(copy(v3), s3, v4), delta);

    // minusTimes and minusTimesEquals
    assertArrayEquals(res_minusTimes_II, minusTimes(v3, v4, s4), delta);
    assertArrayEquals(res_minusTimes_II, minusTimesEquals(copy(v3), v4, s4), delta);

    // timesMinustimes and timesMinustimesEquals
    assertArrayEquals(res_timesMinustimes_II, timesMinusTimes(v3, s3, v4, s4), delta);
    assertArrayEquals(res_timesMinustimes_II, timesMinusTimesEquals(copy(v3), s3, v4, s4), delta);

    // general testing
    final double[] v5 = { 1, 2, 3 };
    final double[] v6 = { 4, 5, 6 };
    final double s5 = 2, s6 = 3;

    // checking that vector - same_vector is zero
    assertArrayEquals(new double[] { 0, 0, 0, }, minus(v5, v5), 0.);
    // consistency check to plus methods
    assertArrayEquals(plus(v5, times(v6, -1)), minus(v5, v6), 0.);
    // consistency check within the minus methods
    assertArrayEquals(minus(v5, times(v6, s6)), minusTimes(v5, v6, s6), 0.);
    assertArrayEquals(minus(times(v5, s5), v6), timesMinus(v5, s5, v6), 0.);
    assertArrayEquals(minus(times(v5, s5), times(v6, s6)), timesMinusTimes(v5, s5, v6, s6), 0.);
  }

  /**
   * Testing scalarProduct and transposeTimes methods of the VMath class.
   */
  @Test
  public void testScalarProduct() {
    // testing scalarProduct and transposeTimes return the same result
    final double[] v1 = { 1.21, 3, 7, -2 };
    final double[] v2 = { 3.5, -1, 4, -7 };
    final double res = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];

    assertEquals(transposeTimes(v1, v2), scalarProduct(v1, v2), 0.);
    assertEquals(res, scalarProduct(v1, v2), 0.);

    // testing scalarProduct and transposeTimes via squareSum
    assertEquals(squareSum(v1), scalarProduct(v1, v1), 0.);
    assertEquals(transposeTimes(v1, v1), scalarProduct(v1, v1), 0.);

    assertEquals(squareSum(v2), transposeTimes(v2, v2), 0.);
    assertEquals(transposeTimes(v2, v2), scalarProduct(v2, v2), 0.);
  }

  /**
   * Testing the timesTranspose(vector, vector) method of {@link VMath} class.
   */
  @Test
  public void testTimesTranspose() {
    final double[] v1 = { 1.21, 3, 7, -2 }, v2 = { 3.5, -1, 4, -7 };
    final double[][] res = { //
        { v1[0] * v2[0], v1[0] * v2[1], v1[0] * v2[2], v1[0] * v2[3] }, //
        { v1[1] * v2[0], v1[1] * v2[1], v1[1] * v2[2], v1[1] * v2[3] }, //
        { v1[2] * v2[0], v1[2] * v2[1], v1[2] * v2[2], v1[2] * v2[3] }, //
        { v1[3] * v2[0], v1[3] * v2[1], v1[3] * v2[2], v1[3] * v2[3] } };
    assertTrue(VMath.equals(timesTranspose(v1, v2), res));

    // testing timesTranspose(vector, vector) via times(matrix, matrix)
    // because times(vector) returns a matrix. This is at the same time a test
    // for transpose if timesTranspose is correct.
    final double[][] m1 = transpose(transpose(TESTVEC));
    final double[][] m2 = transpose(TESTVEC);
    assertTrue(VMath.equals(timesTranspose(TESTVEC, TESTVEC), times(m1, m2)));
  }

  /**
   * Testing the normalizeVector) and normalizeEquals(Vector) methods of the
   * {@link VMath} class.
   */
  @Test
  public void testNormalize() {
    final double[] v1 = copy(TESTVEC), v1_copy = copy(v1);
    final double[] v1_normal = normalize(v1);

    // Test that both methods return a vector with length 1.
    // That more methods ensure the result we use squareSum instead of
    // euclideanLength here
    assertEquals(1, squareSum(v1_normal), 0.);
    assertEquals(1, squareSum(normalizeEquals(v1)), 0.);

    // Check that both methods return the same Vector
    assertArrayEquals(v1_normal, v1, 0.);

    // Check that the normalize Vector times the euclidean length of the
    // original Vector equals the original vector
    assertArrayEquals(v1_copy, times(v1_normal, euclideanLength(v1_copy)), 1e-15);
  }

  /**
   * Testing the cosine angle(Vector, Vector) methods of the VMath class.
   */
  @Test
  public void testAngle() {
    // test case I
    final double[] v1_I = { 1, 0 }, v2_I = { 1, 1 }, v3_I = { 2, 0, 0 };
    assertEquals(MathUtil.SQRTHALF, angle(v2_I, v1_I), 0.);
    assertEquals(MathUtil.SQRTHALF, angle(v1_I, v2_I), 0.);
    assertEquals(1., angle(v3_I, v1_I), 0.);
    assertEquals(1., angle(v1_I, v3_I), 0.);

    // set the origin, no change of data needed
    final double[] origin_I = { 0, 1 };
    assertEquals(MathUtil.SQRTHALF, angle(v2_I, v1_I, origin_I), 0.);
    assertEquals(MathUtil.SQRTHALF, angle(v1_I, v2_I, origin_I), 0.);
    assertEquals(Math.sqrt(0.8), angle(v3_I, v2_I, origin_I), 0.);
    assertEquals(Math.sqrt(0.8), angle(v2_I, v3_I, origin_I), 0.);

    // test case II
    final double[] v1_II = { 1, 0 }, v2_II = { 1, Math.tan(Math.PI / 3) };
    assertEquals(Math.sqrt(.25), angle(v2_II, v1_II), 1e-15);
    assertEquals(Math.sqrt(.25), angle(v1_II, v2_II), 1e-15);

    // change the origin
    final double[] v3_II = { 2, 3 }, v4_II = { 2, 3 + Math.tan(Math.PI / 3) };
    final double[] origin_II = { 1, 3 };
    assertEquals(Math.sqrt(.25), angle(v3_II, v4_II, origin_II), 1e-15);
    assertEquals(Math.sqrt(.25), angle(v4_II, v3_II, origin_II), 1e-15);
  }

  /**
   * Testing the summation methods and the euclidienLength method of VMath
   * class.
   */
  @Test
  public void testSummation() {
    // testing sum
    final double[] v = { 1.21, -3, 2, 3.2, -5, 1 };
    final double res_vsum = v[0] + v[1] + v[2] + v[3] + v[4] + v[5];
    assertEquals(res_vsum, sum(v), 0.);

    assertEquals(1, sum(unitVector(3, 1)), 0.);

    final double[] v1 = { 0.1234512345123456, -0.123451234512345 };
    assertEquals(0, sum(v1), 1e-15);

    // testing squareSum
    final double res_vsumsqare = v[0] * v[0] + v[1] * v[1] + v[2] * v[2] + v[3] * v[3] + v[4] * v[4] + v[5] * v[5];
    assertEquals(res_vsumsqare, squareSum(v), 0.);
    assertEquals(1, squareSum(unitVector(20, 10)), 0.);
    // via transposeTimes and scalarProduct
    assertEquals(transposeTimes(v, v), squareSum(v), 0.);

    // testing euclideanLength
    final double res_veuclid = Math.sqrt(res_vsumsqare);
    assertEquals(res_veuclid, euclideanLength(v), 0.);
    assertEquals(1, euclideanLength(unitVector(3, 1)), 0.);
  }

  /**
   * Testing the transpose(vector) method of VMath class.
   */
  @Test
  public void testTransposed() {
    final double[] v1 = TESTVEC;
    final double[][] v1_t = { v1 };
    assertTrue(VMath.equals(transpose(v1), v1_t));

    final double[] v2 = { 4, 5, 6, 0, 2, 1 };
    final double[][] v2_t = { v2 };
    assertTrue(VMath.equals(transpose(v2), v2_t));
  }

  /**
   * Testing the rotate90Equals(vector) method of VMath class.
   */
  @Test
  public void testRotate90Equals() {
    // simple test case
    final double[] v1 = { 1, 0 };
    final double[] res = { 0, -1 };
    assertArrayEquals(res, rotate90Equals(v1), 0);

    // more complex test case
    final double[] v2 = { 1.21, -2.4 };
    final double[] v2_copy = copy(v2);
    // first rotation -> angle of 90° while cos(90°) = 0
    assertEquals(0.0, angle(v2_copy, rotate90Equals(v2)), 0.);
    final double[] v2_copy1rotate = copy(v2);
    // second rotation -> additive inverse
    assertArrayEquals(times(v2_copy, -1), rotate90Equals(v2), 0.);
    // third rotation -> additive inverse to first rotation
    assertArrayEquals(times(v2_copy1rotate, -1), rotate90Equals(v2), 0.);
    // forth rotation -> identity
    assertArrayEquals(v2_copy, rotate90Equals(v2), 0.);
  }

  /**
   * Testing that the *Equals methods tested in this class work in place and
   * that the other
   * methods tested create a new instance.
   *
   * Tests of methods where the class of the instance returned differs form the
   * class of input are reasonably omitted,
   * when testing reference.
   */
  @Test
  public void testReference() {
    final double[] v1 = { 0 }, v2 = { 0 };
    final double s1 = 0, s2 = 0;

    // testing methods as in testMinus
    assertNotSame(v1, minus(v1, s1));
    assertSame(v1, minusEquals(v1, s1));

    assertNotSame(v1, minus(v1, v2));
    assertSame(v1, minusEquals(v1, v2));

    assertNotSame(v1, timesMinus(v1, s1, v2));
    assertSame(v1, timesMinusEquals(v1, s1, v2));

    assertNotSame(v1, minusTimes(v1, v2, s2));
    assertSame(v1, minusTimesEquals(v1, v2, s2));

    assertNotSame(v1, timesMinusTimes(v1, s1, v2, s2));
    assertSame(v1, timesMinusTimesEquals(v1, s1, v2, s2));

    // testing methods as in testNormalize
    final double[] v3 = { 2 };
    assertNotSame(v3, normalize(v3));
    assertSame(v3, normalizeEquals(v3));

    // testing methods as in testPlus
    assertNotSame(v1, plus(v1, s1));
    assertSame(v1, plusEquals(v1, s1));

    assertNotSame(v1, plus(v1, v2));
    assertSame(v1, plusEquals(v1, v2));

    assertNotSame(v1, timesPlus(v1, s1, v2));
    assertSame(v1, timesPlusEquals(v1, s1, v2));

    assertNotSame(v1, plusTimes(v1, v2, s2));
    assertSame(v1, plusTimesEquals(v1, v2, s2));

    assertNotSame(v1, timesPlusTimes(v1, s1, v2, s2));
    assertSame(v1, timesPlusTimesEquals(v1, s1, v2, s2));

    // testing methods as in testTimes
    assertNotSame(v1, times(v1, s1));
    assertSame(v1, timesEquals(v1, s1));

    // testing methods as in testRotate90Equals
    final double[] v4 = { 1, 0 };
    assertSame(v4, rotate90Equals(v4));
  }

  /**
   * Testing that correct Error is raised when dimension of the input data
   * mismatch the needs of the method.
   * Methods where no error is to be raised are omitted.
   *
   * See {@link VMathOperationsTest#assertDimensionMismatch} for details.
   */
  @Test
  public void testDimensionMismatch() {
    final double[] v_len1 = { 1 }, v_len2 = { 1, 1 }, v_len3 = { 1, 1, 1 };
    double s1 = 1, s2 = 1;

    // methods as in testPlus
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> plus(v_len1, v_len2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> plusEquals(v_len1, v_len2));

    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesPlus(v_len1, s1, v_len2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesPlusEquals(v_len1, s1, v_len2));

    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> plusTimes(v_len1, v_len2, s2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> plusTimesEquals(v_len1, v_len2, s2));

    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesPlusTimes(v_len1, s1, v_len2, s2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesPlusTimesEquals(v_len1, s1, v_len2, s2));

    // methods as in testMinus
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> minus(v_len1, v_len2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> minusEquals(v_len1, v_len2));

    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesMinus(v_len1, s1, v_len2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesMinusEquals(v_len1, s1, v_len2));

    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> minusTimes(v_len1, v_len2, s2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> minusTimesEquals(v_len1, v_len2, s2));

    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesMinusTimes(v_len1, s1, v_len2, s2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> timesMinusTimesEquals(v_len1, s1, v_len2, s2));

    // methods as in testScalarProduct
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> scalarProduct(v_len1, v_len2));
    assertDimensionMismatch(ERR_VEC_DIMENSIONS, () -> transposeTimes(v_len1, v_len2));

    // test rotate90
    assertDimensionMismatch("rotate90Equals is only valid for 2d vectors.", () -> rotate90Equals(v_len1));
    assertDimensionMismatch("rotate90Equals is only valid for 2d vectors.", () -> rotate90Equals(v_len3));
  }
}
