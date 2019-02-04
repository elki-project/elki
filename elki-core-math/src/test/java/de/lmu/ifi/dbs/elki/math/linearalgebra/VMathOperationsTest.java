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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.CholeskyDecompositionTest.TESTMATRIX_L1;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.CholeskyDecompositionTest.TESTMATRIX_L2;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMathMatrixTest.TESTMATRIX;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMathVectorTest.TESTVEC;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/**
 * Test the VMath class methods which are non mathematical operations on vectors
 * or matrixes.
 *
 * @author Merlin Dietrich
 * @since 0.7.5
 */
public final class VMathOperationsTest {
  /**
   * Method for testing that exceptions are raised if dimension mismatches in
   * input data occur.
   * 
   * This test caches Assertion Errors with a given Message if the -ea option is
   * set or rather if assertions are executed. If the -ea option is not set, an
   * ArrayIndexOutOfBoundsException may occur, but out of optimization
   * considerations we don't want to guarantee this exception. So in this case
   * we can't decide if a dimension mismatch has occurred. This is why we need
   * the {@link failWrapper} method.
   * 
   * Let's make an example of usage: <br>
   * Let's take two Vectors v1, v2 with different lengths.
   * So v1 + v2 should not be possible to compute, so we want v1.length to equal
   * v2.length and assert with the {@link ERR_VEC_DIMENSIONS} error
   * Message. If we think of any implementation of a plus(v1, v2) method with
   * vectors as arrays e.g. {@link VMath#plus(double[], double[])},
   * we are going to iterate either over the length of v1 or v2. But with
   * assertions turned of (no -ea set) either v1+v2 or v2+v1 is going to raise
   * an ArrayIndexOutOfBoundsException, while the other is not.
   * 
   * <pre>
   * assertDimensionMismatch({@link VMathVectorTest#ERR_VEC_DIMENSIONS},() -> plus(v1, v2) )
   * </pre>
   * 
   * @param msg Assertion Message to be raised with -ea on
   * @param r runnable of the method to be tested
   */
  protected static void assertDimensionMismatch(String msg, Runnable r) {
    try {
      r.run();
    }
    catch(AssertionError e) {
      assertEquals(msg, e.getMessage());
      return; // If assertions are enabled.
    }
    catch(ArrayIndexOutOfBoundsException e) {
      return; // Expected failure
    }
    // We only guarantee an error if assertions are enabled with -ea.
    // If they are not the next line will do nothing.
    assert failWrapper(msg);
  }

  /**
   * FailWrapper function, to wrap a fail into an assertion.
   * 
   * This is needed to toggle the fail with the -ea option.
   * 
   * @param msg the msg of {@link assertDimensionMismatch}
   * @return ture
   * 
   * @see {@link assertDimensionMismatch}
   */
  private static boolean failWrapper(String msg) {
    throw new AssertionError("Expected AssertionError with message: " + msg);
  }

  /**
   * Test the copy functions of VMath class.
   * 
   * Tested Methods:
   * copy(vector), copy(Matrix), columPackedCopy(Matrix), rowPackedcCopy(Matrix)
   */
  @Test
  public void testCopy() {
    // testing copy(vector) method
    final double[] v1 = { 1, 2, 3, 4 }, v1_res = copy(v1);
    assertArrayEquals(v1, v1_res, 0.);
    assertNotSame(v1, v1_res);

    final double[] v2 = TESTVEC, v2_res = copy(v2);
    assertArrayEquals(v2, v2_res, 0.);
    assertNotSame(v2, v2_res);

    // testing copy(Matrix) method
    final double[][] m1 = TESTMATRIX, m1_res = copy(m1);
    assertTrue(Arrays.deepEquals(m1, m1_res));
    assertNotSame(m1, m1_res);
    for(int i = 0; i < m1.length; i++) {
      assertNotSame(m1[i], m1_res[i]); // Ensure it is a deep copy.
    }

    final double[][] m2 = { { 0, 1, 0.123451234512345, 2 }, { 2, 3, 4.123451234512345, -1 } };
    final double[][] m2_res = copy(m2);
    assertTrue(Arrays.deepEquals(m2, m2_res));
    assertNotSame(m2, m2_res);
    for(int i = 0; i < m2.length; i++) {
      assertNotSame(m2[i], m2_res[i]); // Ensure it is a deep copy.
    }

    // testing columPackedCopy(Matrix) method
    final double[] m2_colpack = { 0, 2, 1, 3, 0.123451234512345, 4.123451234512345, 2, -1 };

    final double[] m2_colpackres = columnPackedCopy(m2);
    assertArrayEquals(columnPackedCopy(m2), m2_colpack, 0.);
    assertNotSame(m2, m2_colpackres);

    // testing rowPackedCopy(Matrix) method
    final double[] m2_rowpack = { 0, 1, 0.123451234512345, 2, 2, 3, 4.123451234512345, -1 };
    final double[] m2_rowpackres = rowPackedCopy(m2);
    assertArrayEquals(rowPackedCopy(m2), m2_rowpack, 0.);
    assertNotSame(m2, m2_rowpackres);
  }

  /**
   * Testing the hashcode() methods of VMath class.
   */
  @Test
  public void testHashcode() {
    final double[] v = TESTVEC;
    final double[][] m = TESTMATRIX;
    assertEquals(Arrays.hashCode(v), VMath.hashCode(v), 0.);
    assertEquals(Arrays.deepHashCode(m), VMath.hashCode(m), 0.);
  }

  /**
   * Testing the clear(vector), clear(matrix) methods of VMath class.
   */
  @Test
  public void testClear() {
    // test clear(vector)
    final double[] v = copy(TESTVEC);
    clear(v);
    for(double x : v) {
      assertEquals(0., x, 0.);
    }
    assertTrue(VMath.equals(v, new double[5]));

    // test clear(matrix)
    final double[][] m = copy(TESTMATRIX);
    clear(m);
    for(double[] row : m) {
      for(double x : row) {
        assertEquals(0., x, 0.);
      }
    }
    assertTrue(VMath.equals(new double[4][5], m));
  }

  /**
   * Testing the equals methods of VMath class, which compare vectors or
   * matrixes.
   */
  @Test
  public void testEquals() {
    // equals(Vector)
    final double[] v1 = { 2, 4, 3, 0, -5, 9 };
    // copy made by hand to be independent of copy module
    final double[] v1_copy = { 2, 4, 3, 0, -5, 9 };

    assertNotSame("Not shallow different.", v1, v1_copy);
    assertArrayEquals(v1, v1, 0.);
    assertArrayEquals(v1, v1_copy, 0.);
    assertFalse(VMath.equals(unitVector(6, 2), v1));

    // equals(Matrix)
    final double[][] m1 = { { 1, 2, 3 }, { 7, 3, 9 }, { 0, 2, 1 }, { 20, -5, 4 }, { -3, 0, 3 }, { 1, 1, 1 } };
    // make copy of by hand to be independent of copy module
    final double[][] m1_copy = { { 1, 2, 3 }, { 7, 3, 9 }, { 0, 2, 1 }, { 20, -5, 4 }, { -3, 0, 3 }, { 1, 1, 1 } };

    assertNotSame("Not shallow different.", m1, m1_copy);
    assertTrue(VMath.equals(m1, m1));
    assertTrue(VMath.equals(m1, m1_copy));

    // Modify the copy, check it's no longer equal:
    m1_copy[0][0] = 3.14;
    assertFalse(VMath.equals(m1, m1_copy));
    m1_copy[0][0] = Double.NaN;
    assertFalse(VMath.equals(m1, m1_copy));
    assertFalse(VMath.equals(identity(6, 3), m1));
  }

  /**
   * Testing the almostEquals methods of VMath class.
   * 
   * Note that almostEquals(m1,m2) is equivalent to almostEquals(m1,m2,
   * {@link VMath#DELTA})
   */
  @Test
  public void testMatrixAlmosteq() {
    final double[][] m1 = copy(TESTMATRIX);
    final double[][] m2 = copy(TESTMATRIX);
    assertNotSame(m1, m2);

    // basic function test
    assertTrue(VMath.equals(m1, m1));
    assertTrue(VMath.equals(m1, m2));
    assertFalse(VMath.equals(m1, null));
    assertFalse(VMath.equals(null, m2));
    assertTrue(almostEquals(m1, m1, 0.));
    assertTrue(almostEquals(m1, m2, 0.));
    assertFalse(almostEquals(null, m2, 0.));
    assertFalse(almostEquals(m1, null, 0.));
    assertFalse(VMath.equals(m1, identity(4, 5)));
    assertFalse(almostEquals(m1, identity(4, 5), 0.));
    assertTrue(almostEquals(m1, identity(4, 5), 5)); // 5 = max difference.

    // fail if dimensions mismatch
    assertFalse(almostEquals(m1, new double[][] { { 1 } }));

    // testing that assert fails if difference d > maxdelta else not
    // we test with increasing maxdelta and work on the same data
    // maxdelta = EPSILON
    double[][] res_diff = copy(m1);
    double EPSILON = 1e-8;
    res_diff[3][3] += 1.5 * EPSILON;
    assertFalse(almostEquals(m1, res_diff, EPSILON));
    res_diff[3][3] -= EPSILON;
    assertTrue(almostEquals(m1, res_diff, EPSILON));

    // maxdelta DELTA of VMath respectively 1E-5
    res_diff[0][4] += 1.5E-5;
    assertFalse(almostEquals(m1, res_diff));
    res_diff[0][4] -= 1E-5;
    assertTrue(almostEquals(m1, res_diff));

    // maxdelta = 1E10
    res_diff[2][1] += 1.5E10;
    assertFalse(almostEquals(m1, res_diff, 1E10));
    res_diff[2][1] -= 1E10;
    assertTrue(almostEquals(m1, res_diff, 1E10));
  }

  /**
   * Testing the {@link VMath#unitVector(int, int)} method.
   */
  @Test
  public void testUnitVector() {
    // test if unitVector is returned for dimension 3
    assertArrayEquals(new double[] { 1, 0, 0 }, unitVector(3, 0), 0.);
    assertArrayEquals(new double[] { 0, 1, 0 }, unitVector(3, 1), 0.);
    assertArrayEquals(new double[] { 0, 0, 1 }, unitVector(3, 2), 0.);

    // for dimension 5
    assertArrayEquals(new double[] { 1, 0, 0, 0, 0 }, unitVector(5, 0), 0.);
    assertArrayEquals(new double[] { 0, 1, 0, 0, 0 }, unitVector(5, 1), 0.);
    assertArrayEquals(new double[] { 0, 0, 1, 0, 0 }, unitVector(5, 2), 0.);
    assertArrayEquals(new double[] { 0, 0, 0, 1, 0 }, unitVector(5, 3), 0.);
    assertArrayEquals(new double[] { 0, 0, 0, 0, 1 }, unitVector(5, 4), 0.);
  }

  /**
   * Testing the getMatrix, getCol and getRow methods of VMath class.
   */
  @Test
  public void testGet() {
    // testmatrix with dimensions 7x10 where every entry is unique
    final double[][] m1 = { //
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, //
        { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }, //
        { 20, 21, 22, 23, 24, 25, 26, 27, 28, 29 }, //
        { 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 }, //
        { 40, 41, 42, 43, 44, 45, 46, 47, 48, 49 }, //
        { 50, 51, 52, 53, 54, 55, 56, 57, 58, 59 }, //
        { 60, 61, 62, 63, 64, 65, 66, 67, 68, 69 } };
    // arrays of row and columns indices
    final int[] r = { 0, 3, 5 }, c = { 1, 3, 3, 9 };
    // test getMatrix(Matrix, rows, columns)
    final double[][] sub1 = { //
        { m1[r[0]][c[0]], m1[r[0]][c[1]], m1[r[0]][c[2]], m1[r[0]][c[3]] }, //
        { m1[r[1]][c[0]], m1[r[1]][c[1]], m1[r[1]][c[2]], m1[r[1]][c[3]] }, //
        { m1[r[2]][c[0]], m1[r[2]][c[1]], m1[r[2]][c[2]], m1[r[2]][c[3]] } };

    assertTrue(VMath.equals(getMatrix(m1, r, c), sub1));

    // test getMatrix(Matrix, rowstart, rowend , columns)
    final double[][] sub2 = { //
        { m1[2][c[0]], m1[2][c[1]], m1[2][c[2]], m1[2][c[3]] }, //
        { m1[3][c[0]], m1[3][c[1]], m1[3][c[2]], m1[3][c[3]] }, //
        { m1[4][c[0]], m1[4][c[1]], m1[4][c[2]], m1[4][c[3]] }, //
        { m1[5][c[0]], m1[5][c[1]], m1[5][c[2]], m1[5][c[3]] } };

    assertTrue(VMath.equals(getMatrix(m1, 2, 6, c), sub2));

    // test getMatrix(Matrix, rows, columnstart, columnend)
    final double[][] sub3 = { //
        { m1[r[0]][4], m1[r[0]][5], m1[r[0]][6], m1[r[0]][7] }, //
        { m1[r[1]][4], m1[r[1]][5], m1[r[1]][6], m1[r[1]][7] }, //
        { m1[r[2]][4], m1[r[2]][5], m1[r[2]][6], m1[r[2]][7] } };

    assertTrue(VMath.equals(getMatrix(m1, r, 4, 8), sub3));

    // test getMatrix(Matrix, rowstart, rowend, columnstart, columnend)
    final double[][] sub4 = { //
        { m1[0][6], m1[0][7], m1[0][8] }, //
        { m1[1][6], m1[1][7], m1[1][8] }, //
        { m1[2][6], m1[2][7], m1[2][8] } };

    assertTrue(VMath.equals(getMatrix(m1, 0, 3, 6, 9), sub4));

    // cross methods and general testing
    assertTrue(VMath.equals(getMatrix(m1, 0, getRowDimensionality(m1), 0, getColumnDimensionality(m1)), m1));

    final int[] riter = { 3, 4, 5 };
    final int[] citer = { 0, 1, 2 };
    assertTrue(VMath.equals(getMatrix(m1, riter[0], riter[riter.length - 1] + 1, citer[0], citer[citer.length - 1] + 1), getMatrix(m1, riter, citer)));
    assertTrue(VMath.equals(getMatrix(m1, riter, citer[0], citer[citer.length - 1] + 1), getMatrix(m1, riter, citer)));
    assertTrue(VMath.equals(getMatrix(m1, riter[0], riter[riter.length - 1] + 1, citer), getMatrix(m1, riter, citer)));

    // test getCol and getRow
    // we assume transpose to be correct
    for(int i = 0; i < TESTMATRIX.length; i++) {
      double[] v = TESTMATRIX[i];
      assertArrayEquals(v, getRow(TESTMATRIX, i), 0.);
      assertNotSame(v, getRow(TESTMATRIX, i));
      assertArrayEquals(v, getCol(transpose(TESTMATRIX), i), 0.);
    }
  }

  /**
   * Testing the getRowDimensionality and getColumnDimensionality methods of
   * VMath class.
   */
  @Test
  public void testGetDimensionality() {
    final double[][] m3 = new double[3][5];
    assertEquals(3, getRowDimensionality(m3));
    assertEquals(3, getColumnDimensionality(transpose(m3)));
    assertEquals(5, getColumnDimensionality(m3));
    assertEquals(5, getRowDimensionality(transpose(m3)));
  }

  /**
   * Testing the diagonal and getdiagonal methods of VMath class.
   */
  @Test
  public void testDiagonal() {
    final double[] m = TESTVEC;
    final double[][] m_diag = { //
        { m[0], 0, 0, 0, 0 }, //
        { 0, m[1], 0, 0, 0 }, //
        { 0, 0, m[2], 0, 0 }, //
        { 0, 0, 0, m[3], 0 }, //
        { 0, 0, 0, 0, m[4] } };
    assertTrue(almostEquals(diagonal(m), m_diag));

    final double[] dia_TEST = { TESTMATRIX[0][0], TESTMATRIX[1][1], TESTMATRIX[2][2], TESTMATRIX[3][3] };
    assertArrayEquals(dia_TEST, getDiagonal(TESTMATRIX), 0.);

    // if diagonal is correct this is a test for getDiagonal
    // if getDiagonal is correct a test for diagonal
    final double[] dia = { -2, 0, 1.21, 4, 7 };
    assertArrayEquals(dia, getDiagonal(diagonal(dia)), 0.);
  }

  /**
   * Testing setMatrix, setCol and setRow methods of VMath class.
   * 
   * Since the get-methods of the VMath class are tested independently of set in
   * {@link #testGet()},
   * we mainly test here via those get Methods.
   */
  @Test
  public void testSet() {
    // Initialize testmatrix
    final double[][] m1 = new double[20][25];

    // test setMatrix(Matrix, rows, columns, tosetMatrix)
    // column an row indexes. Assert that you don't address the same column/row
    // twice.
    final int[] row_index = { 3, 5, 10, 2, 11, 19, 8 };
    final int[] col_index = { 0, 10, 7, 19, 3, 6, 23, 5, 4 };
    // inputmatix with dimensions 7x9
    final double[][] sub1 = randomMatrix(7, 9, 0L);
    setMatrix(m1, row_index, col_index, sub1);
    assertTrue(VMath.equals(getMatrix(m1, row_index, col_index), sub1));

    // test setMatrix(Matrix, rowstart, rowend, columns, tosetMatrix)
    // testmatix with dimensions 5x9
    final double[][] sub2 = randomMatrix(5, 9, 3L);
    setMatrix(m1, 4, 9, col_index, sub2);
    assertTrue(VMath.equals(getMatrix(m1, 4, 9, col_index), sub2));

    // clear matrix for next test
    clear(m1);

    // test setMatrix(Matrix, rows, colstart, colend, tosetMatrix)
    // testmatix with dimensions 7x15
    final double[][] sub3 = randomMatrix(7, 15, 2L);

    setMatrix(m1, row_index, 2, 17, sub3);
    assertTrue(VMath.equals(getMatrix(m1, row_index, 2, 17), sub3));

    // test setMatrix(Matrix, rowstart, rowend, colstart, colend, tosetMatrix)
    // testmatix with dimensions 7x3
    final double[][] sub4 = randomMatrix(7, 3, 1L);

    setMatrix(m1, 0, 7, 16, 19, sub4);
    assertTrue(almostEquals(getMatrix(m1, 0, 7, 16, 19), sub4));

    // check that setting a full matrix
    final double[][] m2 = TESTMATRIX;
    final double[][] res1 = new double[getRowDimensionality(m2)][getColumnDimensionality(m2)];
    setMatrix(res1, 0, getRowDimensionality(m2), 0, getColumnDimensionality(m2), m2);
    assertTrue(VMath.equals(res1, m2));

    // check that different the different setMatrix methods to the same if
    // exchangeable
    final int[] riter = { 0, 1, 2, 3 }, citer = { 0, 1, 2, 3, 4 };
    final double[][] res2 = new double[riter.length][citer.length];

    setMatrix(res2, riter[0], riter[riter.length - 1] + 1, citer[0], citer[citer.length - 1] + 1, m2);
    assertTrue(VMath.equals(res2, getMatrix(m2, riter, citer)));
    clear(res2);

    setMatrix(res2, riter, citer[0], citer[citer.length - 1] + 1, m2);
    assertTrue(VMath.equals(res2, getMatrix(m2, riter, citer)));
    clear(res2);

    setMatrix(res2, riter[0], riter[riter.length - 1] + 1, citer, m2);
    assertTrue(VMath.equals(res2, getMatrix(m2, riter, citer)));

    // testing setCol and setRow
    final double[] col = TESTVEC, row = TESTVEC;

    final double[][] m3 = new double[row.length][col.length];

    for(int c = 0; c < getRowDimensionality(m3); c++) {
      // set column c of m to col
      setCol(m3, c, col);
      // assert that column c of m is col via getCol
      assertArrayEquals(col, getCol(m3, c), 0.);
    }

    for(int r = 0; r < getColumnDimensionality(m3); r++) {
      // set column c of m to col
      setRow(m3, r, row);
      // assert that row r of m is row via getRow
      assertArrayEquals(row, getRow(m3, r), 0.);
    }
  }

  /**
   * Generate a matrix with random values.
   * 
   * @param rows Number of rows
   * @param cols Number of cols
   * @param seed Random seed
   * @return Matrix
   */
  private double[][] randomMatrix(int rows, int cols, long seed) {
    Random rnd = new Random(seed);
    double[][] m = new double[rows][cols];
    for(int r = 0; r < rows; r++) {
      for(int c = 0; c < cols; c++) {
        m[r][c] = rnd.nextDouble();
      }
    }
    return m;
  }

  /**
   * Testing the appendColums method of VMath class.
   */
  @Test
  public void testAppendColums() {
    final double[][] m1 = { //
        { 1, 2, 3 }, //
        { 1, 2, 3 }, //
        { 1, 2, 3 } };
    final double[][] m2 = { //
        { 4, 5, 6, 7 }, //
        { 4, 5, 6, 7 }, //
        { 4, 5, 6, 7 } };
    final double[][] m1_res = { //
        { m1[0][0], m1[0][1], m1[0][2], m2[0][0], m2[0][1], m2[0][2], m2[0][3] }, //
        { m1[1][0], m1[1][1], m1[1][2], m2[1][0], m2[1][1], m2[1][2], m2[1][3] }, //
        { m1[2][0], m1[2][1], m1[2][2], m2[2][0], m2[2][1], m2[2][2], m2[2][3] } };

    assertTrue(VMath.equals(appendColumns(m1, m2), m1_res));
  }

  /**
   * Testing that the *Equals methods tested in this class work in place and
   * that the other methods tested create a new instance.
   * 
   * Tests of methods where the class of the instance returned differs form the
   * class of input are reasonably omitted, when testing reference. We omit the
   * copy methods as well because the only testing made there is the reference.
   */
  @Test
  public void testReference() {
    final double[][] m1 = { { 1 } };

    // testing the appendColums method as it is now: not working in place
    assertNotSame(m1, appendColumns(m1, m1));

    // testing that methods as in testGet return new copies, not references
    final double[] inner = { 1 };
    final double[][] m2 = { inner };
    // assert that inner reference is possible
    assertSame(inner, m2[0]);
    assertNotSame(m1, inner);

    final int[] rows = { 0 }, cols = { 0 };
    final int c0 = 0, c1 = 1, r0 = 0, r1 = 1;

    assertNotSame(inner, getMatrix(m2, rows, cols)[0]);
    assertNotSame(inner, getMatrix(m2, r0, r1, cols)[0]);
    assertNotSame(inner, getMatrix(m2, rows, c0, c1)[0]);
    assertNotSame(inner, getMatrix(m2, r0, r1, c0, c1)[0]);

    assertNotSame(inner, getRow(m2, r0));

    // testing that the methods as in testSet, set the rows in the submatrix to
    // refer to a new instance.
    final double[][] m3 = new double[1][1];

    setMatrix(m3, rows, cols, m2);
    assertNotSame(inner, m3[0]);
    setMatrix(m3, r0, r1, cols, m2);
    assertNotSame(inner, m3[0]);
    setMatrix(m3, rows, c0, c1, m2);
    assertNotSame(inner, m3[0]);
    setMatrix(m3, r0, r1, c0, c1, m2);
    assertNotSame(inner, m3[0]);

    setRow(m3, r0, inner);
    assertNotSame(inner, m3[0]);
  }

  /**
   * Testing that correct Error is raised when dimension of the input data
   * mismatch the needs of the method.
   * Methods where no error is to be raised are omitted.
   *
   * See {@link assertDimensionMismatch} for details.
   */
  @Test
  public void testDimensionMismatch() {
    // unitVector only use index out of Bound exception in
    // assertDimensionMismatch
    assertDimensionMismatch("0", () -> unitVector(0, 0));
    assertDimensionMismatch("4", () -> unitVector(4, 4));
    assertDimensionMismatch("15", () -> unitVector(10, 15));

    // testing the methods as in testGet
    final int[] r = { 5 }, c = { 5 };
    final int r1 = 5, c1 = 5;
    assertDimensionMismatch("5", () -> getMatrix(unitMatrix(2), r, c));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> getMatrix(unitMatrix(2), 0, r1, c));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> getMatrix(unitMatrix(2), r, 0, c1));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> getMatrix(unitMatrix(2), 0, r1, 0, c1));
    assertDimensionMismatch("5", () -> getCol(unitMatrix(2), c1));
    assertDimensionMismatch("5", () -> getRow(unitMatrix(2), r1));

    // testing the methods as in testSet
    assertDimensionMismatch("5", () -> setMatrix(unitMatrix(2), r, c, unitMatrix(6)));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> setMatrix(unitMatrix(2), 0, r1, c, unitMatrix(6)));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> setMatrix(unitMatrix(2), r, 0, c1, unitMatrix(6)));
    assertDimensionMismatch(ERR_MATRIX_DIMENSIONS, () -> setMatrix(unitMatrix(2), 0, r1, 0, c1, unitMatrix(6)));
    assertDimensionMismatch(ERR_DIMENSIONS, () -> setCol(unitMatrix(2), c1, unitVector(6, 0)));
    assertDimensionMismatch(ERR_DIMENSIONS, () -> setRow(unitMatrix(2), r1, unitVector(6, 0)));
  }

  @Test
  public void testSolve() {
    final double[] a = { 3, 7, 7, 7, -7, 7, 7, 7, 7, 7 };
    final double[] b = { -2, 3, 5, 1, 8, 1, 1, 1, 1, 1 };
    final double[][] m = transpose(new double[][] { a, b });

    final double[][] A1 = timesTranspose(TESTMATRIX_L1, TESTMATRIX_L1);
    final double[][] A2 = timesTranspose(TESTMATRIX_L2, TESTMATRIX_L2);

    final double[] a1 = solve(A1, a);
    final double[] a2 = solve(A2, a);
    final double[][] a1t = solve(A1, transpose(transpose(a)));
    final double[][] a2t = solve(A2, transpose(transpose(a)));
    final double[] b1 = solve(A1, b);
    final double[] b2 = solve(A2, b);
    final double[][] b1t = solve(A1, transpose(transpose(b)));
    final double[][] b2t = solve(A2, transpose(transpose(b)));
    final double[][] m1 = solve(A1, m);
    final double[][] m2 = solve(A2, m);

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
}
