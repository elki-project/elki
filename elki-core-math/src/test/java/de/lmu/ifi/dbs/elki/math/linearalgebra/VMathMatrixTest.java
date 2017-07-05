/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMathOperationsTest.assertDimensionMismatch;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test the VMath class methods operating on matrixs.
 *
 * @author Merlin Dietrich
 * 
 */
public class VMathMatrixTest {
  
  /**
   * Nonsymmetric 4 x 5 (rows x columns) TESTMATRIX
   */
  protected static final double[][] TESTMATRIX = {
      {1,2,3,4,5}, 
      {2,3,4,5,1}, 
      {3,4,5,1,2}, 
      {4,5,1,2,3} };
  
  /**
   * 1x1 Matrix testing for dimension mismatch testing.
   */
  protected static final double[][] DIMTESTMATRIX = {{1}};
  
  protected static final double EPSILON = 1E-15;
  
  /**
   * Testing the transposed(Matrix) method of VMath class.
   */
  @Test
  public void testMatrixTransposed() {
    
    
    final double[][] m1 = TESTMATRIX;
 
    final double[][] res_case1 = {
          {m1[0][0], m1[1][0], m1[2][0], m1[3][0]},
          {m1[0][1], m1[1][1], m1[2][1], m1[3][1]},
          {m1[0][2], m1[1][2], m1[2][2], m1[3][2]},
          {m1[0][3], m1[1][3], m1[2][3], m1[3][3]},
          {m1[0][4], m1[1][4], m1[2][4], m1[3][4]} 
    };
    
    final double[][] out_case1 = transpose(m1);
    assertThat(out_case1, is(equalTo(res_case1)));
    assertNotSame(m1, out_case1);
    
    
    // FIXME: Question: How many testcases needed, put in ref class?
    
  }
  
  /**
   * Testing the Matrix plus operations of VMath class.
   */
  @Test
  public void testPlus() {
    // TODO: implement plus(m1, m2); plusEquals(m1, m2); plusTimes(m1, m2, s2) ... as in VectorPLUS 
  }
  
  /**
   * Testing the Matrix minus operations of VMath class.
   */
  @Test
  public void testMinus() {
    // TODO: implement minus(m1, m2) ... as in VectorMinus 
  }
  
  /**
   * Testing the Matrix,Scalar multiplication methods times, timesEquals of VMath class. 
   */
  @Test
  public void testMatrixScalarMultiplication() {
    // TODO: comment
    final double[][] m1 = TESTMATRIX;
    
    final double[][] m1_times_one_third = {
        {1/3*m1[0][0], 1/3*m1[0][1], 1/3*m1[0][2], 1/3*m1[0][3], 1/3*m1[0][4]},
        {1/3*m1[1][0], 1/3*m1[1][1], 1/3*m1[1][2], 1/3*m1[1][3], 1/3*m1[1][4]},
        {1/3*m1[2][0], 1/3*m1[2][1], 1/3*m1[2][2], 1/3*m1[2][3], 1/3*m1[2][4]},
        {1/3*m1[3][0], 1/3*m1[3][1], 1/3*m1[3][2], 1/3*m1[3][3], 1/3*m1[3][4]}};
    
    assertTrue(almostEquals(m1_times_one_third, times(m1, 1/3), EPSILON));
    assertTrue(almostEquals(m1_times_one_third, timesEquals(m1, 1/3), EPSILON));
    
    final double[][] m1_times_zero = {
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0}};

    assertThat(times(m1, 0), is(equalTo(m1_times_zero )));
    assertThat(timesEquals(m1, 0), is(equalTo(m1_times_zero )));
    
    assertThat(times(m1, 1), is(equalTo(m1)));
    assertThat(timesEquals(m1, 1), is(equalTo(m1)));
  }
  
  /**
   * Testing the Matrix, Matrix multiplications methods of VMath class.
   * <p>
   * The following VMath methods are tested:<br>
   * times, transposeTimesTranspose, timesTranspose, transposeTimes
   */
  @Test
  public void testMatrixMatrixMultiplication() {
    final double[][] m1 = TESTMATRIX;
    // TODO: implement transposed manually or assume transpose method to be correct
    final double[][] m1_t = transpose(m1);
    
    // check times(Matrix, id) = Matrix = times(id, Matrix) 
    assertThat(times(m1, unitMatrix(5) ), is(equalTo(m1)));
    assertThat(times(unitMatrix(4), m1 ), is(equalTo(m1)));
    
    // check transposeTimesTranspose(Matrix, id) = transpose(Matrix) = transposeTimesTranspose(id, Matrix) 
    assertThat(transposeTimesTranspose(m1, unitMatrix(4) ), is(equalTo(m1_t)));
    assertThat(transposeTimesTranspose(unitMatrix(5), m1 ), is(equalTo(m1_t)));
    
    final double[][] m1_times_m1transposed = { 
        {transposeTimes(m1[0], m1[0]), transposeTimes(m1[0], m1[1]), transposeTimes(m1[0], m1[2]), transposeTimes(m1[0], m1[3]) },
        {transposeTimes(m1[1], m1[0]), transposeTimes(m1[1], m1[1]), transposeTimes(m1[1], m1[2]), transposeTimes(m1[1], m1[3]) },
        {transposeTimes(m1[2], m1[0]), transposeTimes(m1[2], m1[1]), transposeTimes(m1[2], m1[2]), transposeTimes(m1[2], m1[3]) },
        {transposeTimes(m1[3], m1[0]), transposeTimes(m1[3], m1[1]), transposeTimes(m1[3], m1[2]), transposeTimes(m1[3], m1[3]) } };
    
    // check timesTranspose without not using a matrix methods times
    assertThat(timesTranspose(m1, m1) , is(equalTo(m1_times_m1transposed)));
    
    // check timesTranspose without not using a vector method transposeTimes
    // this is at the same time a test for the times method assuming the test before succeeded.
    assertThat(timesTranspose(m1, m1) , is(equalTo(times(m1, m1_t))));
    // and the following analog a test for the transposeTimesTranspose method
    assertThat(timesTranspose(m1, m1) , is(equalTo(transposeTimesTranspose(m1_t, m1))));
    
    
    final double[][] m1transposed_times_m1 = {
        {transposeTimes(m1_t[0], m1_t[0]), transposeTimes(m1_t[0], m1_t[1]), transposeTimes(m1_t[0], m1_t[2]), transposeTimes(m1_t[0], m1_t[3]), transposeTimes(m1_t[0], m1_t[4]) },
        {transposeTimes(m1_t[1], m1_t[0]), transposeTimes(m1_t[1], m1_t[1]), transposeTimes(m1_t[1], m1_t[2]), transposeTimes(m1_t[1], m1_t[3]), transposeTimes(m1_t[1], m1_t[4]) },
        {transposeTimes(m1_t[2], m1_t[0]), transposeTimes(m1_t[2], m1_t[1]), transposeTimes(m1_t[2], m1_t[2]), transposeTimes(m1_t[2], m1_t[3]), transposeTimes(m1_t[2], m1_t[4]) },
        {transposeTimes(m1_t[3], m1_t[0]), transposeTimes(m1_t[3], m1_t[1]), transposeTimes(m1_t[3], m1_t[2]), transposeTimes(m1_t[3], m1_t[3]), transposeTimes(m1_t[3], m1_t[4]) },
        {transposeTimes(m1_t[4], m1_t[0]), transposeTimes(m1_t[4], m1_t[1]), transposeTimes(m1_t[4], m1_t[2]), transposeTimes(m1_t[4], m1_t[3]), transposeTimes(m1_t[4], m1_t[4]) } };
 
    // check transposeTimes without not using a matrix methods times
    // without transpose and times
    assertThat(transposeTimes(m1, m1) , is(equalTo(m1transposed_times_m1)));
    
    // check transposeTimes without using a vector method timesTransposed
    // this is as well a test for the times method assuming the test before succeeded.
    assertThat(transposeTimes(m1, m1) , is(equalTo(times(m1_t, m1))));
    // and the following analog a test for the transposeTimesTranspose method
    assertThat(transposeTimes(m1, m1) , is(equalTo(transposeTimesTranspose(m1, m1_t))));

    // TODO extra testcase for times and  transposedTimestransposed

    //    final double[][] m2 = {
    //        {1.21, 2000},
    //        {0   ,-1   },
    //        {7   , 0   } };
    //
    //    final double[][] m3 = {
    //        {1.21, 2000, 0},
    //        {0   ,-1   , 7} };
  }
  
  /**
   * Testing the Matrix, Vector multiplications methods of VMath class. 
   * <p>
   * The following VMath methods are tested:<br>
   * times(matrix, vector), times(vector, matrix), transposeTimes(matrix, vector), transposeTimes(vector, matrix),
   * timesTranspose(vector, matrix), transposeTimesTimes(vector, matrix, vector)
   */
  @Test
  public void testMatrixVectorMultiplication() {
    // TODO: more testcases via eigenvalues. New class for new Times and Transposed 
    final double[][] m1 = {
        {1.21, 2000},
        {0   ,-1   },
        {7   , 0   } };
 
    // we assume the transpose(matrix) method to be correct.
    final double[][] m1_t = transpose(m1);
    
    final double delta = 1E-11;
    
    //  times(m1, v2)
    assertArrayEquals(m1_t[0], times(m1, unitVector(2, 0)) , delta);
    assertArrayEquals(m1_t[1], times(m1, unitVector(2, 1)) , delta);

    //  transposeTimes(m1, v2);
    assertArrayEquals(m1[0], transposeTimes(m1, unitVector(3, 0)) , delta);
    assertArrayEquals(m1[1], transposeTimes(m1, unitVector(3, 1)) , delta);
    assertArrayEquals(m1[2], transposeTimes(m1, unitVector(3, 2)) , delta);
    //  transposeTimesTimes(a, B, c);
    
    // TODO: implement below, and maybe write better documentation in VMath class
    // times(v1, m2) sames a timesTransposed(Vector, Vector) should be same as times (vector, transpose(vector))
    //   this is why this method exists                                                                                       |- is aMatrix
    // basically same timesTransposed as above but second vector given as matrix bzw m1 = transpose(transpose(vector))
    
    // timesTranspose(v1, m2); transposedTimes(vector, Matrix) same as transpose(transposeTimes(matrix, vector))
    // problem may be assertation not m2.lenth but columndimension needed
    
    // TODO Vortrag: Warum so viele Verschieden Multiplicationen Querverweise.
     
  }
    
  //TODO: implement Matrix Ref class
  
  /**
   * Testing the unitMatrix and the identity method of VMath class.
   */
  @Test
  public void testUnitMatrix() {
    // test unitMatrix(dim) and unitMatrix(dim) equals identity(dim, dim)
    final double[][] m_unit = {{1,0,0,0,0},
                               {0,1,0,0,0},
                               {0,0,1,0,0},
                               {0,0,0,1,0},
                               {0,0,0,0,1} };

    assertThat(unitMatrix(5), is(equalTo(m_unit)));
    assertThat(identity(5, 5), is(equalTo(m_unit)));
 
    // test identity with dimensions 3x5 and  5x3
    final double[][] m_identity3x5 = {{1,0,0,0,0},
                                      {0,1,0,0,0},
                                      {0,0,1,0,0}};

    // TODO: implement Dimension Missmatch probably in extra class
    // FIXME: randomize
    assertThat(identity(3, 5), is(equalTo(m_identity3x5)));   
    assertThat(identity(5, 3), is(equalTo(transpose(m_identity3x5))));  
     
  }
  
  /**
   * Testing the getMatrix, getCol and getRow methods of VMath class.
   */
  @Test
  public void testGet() {
    
    // testmatrix with dimensions 7x10  where every entry is unique
    final double[][] m1 = {
        {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9},
        { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19},
        { 20, 21, 22, 23, 24, 25, 26, 27, 28, 29},
        { 30, 31, 32, 33, 34, 35, 36, 37, 38, 39},
        { 40, 41, 42, 43, 44, 45, 46, 47, 48, 49},
        { 50, 51, 52, 53, 54, 55, 56, 57, 58, 59},
        { 60, 61, 62, 63, 64, 65, 66, 67, 68, 69} };
    // TODO: randomize
    // array of row indices
    final int[] r = {0,3,5};
    // array of column indices
    final int[] c = {1,3,3,9};
    
    // test getMatrix(Matrix, rows, columns)
    final double[][] sub1 = {
        {m1[r[0]][c[0]], m1[r[0]][c[1]], m1[r[0]][c[2]], m1[r[0]][c[3]]},
        {m1[r[1]][c[0]], m1[r[1]][c[1]], m1[r[1]][c[2]], m1[r[1]][c[3]]},
        {m1[r[2]][c[0]], m1[r[2]][c[1]], m1[r[2]][c[2]], m1[r[2]][c[3]]} };

    assertThat(getMatrix(m1, r, c) , is(equalTo(sub1))); 
    
    // test getMatrix(Matrix, rowstart, rowend , columns)
    final double[][] sub2 = {
        {m1[2][c[0]], m1[2][c[1]], m1[2][c[2]], m1[2][c[3]]},
        {m1[3][c[0]], m1[3][c[1]], m1[3][c[2]], m1[3][c[3]]},
        {m1[4][c[0]], m1[4][c[1]], m1[4][c[2]], m1[4][c[3]]},
        {m1[5][c[0]], m1[5][c[1]], m1[5][c[2]], m1[5][c[3]]} };
    
    assertThat(getMatrix(m1, 2, 5, c) , is(equalTo(sub2)));
    
    // test getMatrix(Matrix, rows, columnstart, columnend)
    final double[][] sub3 = {
        {m1[r[0]][4], m1[r[0]][5], m1[r[0]][6], m1[r[0]][7]},
        {m1[r[1]][4], m1[r[1]][5], m1[r[1]][6], m1[r[1]][7]},
        {m1[r[2]][4], m1[r[2]][5], m1[r[2]][6], m1[r[2]][7]} };

    assertThat(getMatrix(m1, r, 4, 7) , is(equalTo(sub3)));
    
    // test getMatrix(Matrix, rowstart, rowend, columnstart, columnend)
    final double[][] sub4 = {
    {m1[0][6], m1[0][7], m1[0][8]},
    {m1[1][6], m1[1][7], m1[1][8]},
    {m1[2][6], m1[2][7], m1[2][8]} };
     
    assertThat(getMatrix(m1, 0, 2, 6, 8) , is(equalTo(sub4)));
    
    // cross mehtods and general testing FIXME: maybe randomize
    assertThat(getMatrix(m1, 0, getRowDimensionality(m1)-1, 0, getColumnDimensionality(m1)-1) , is(equalTo(m1)));

    final int[] riter = {3,4,5};
    final int[] citer = {0,1,2};
    assertThat(getMatrix(m1, riter[0], riter[riter.length-1], citer[0], citer[citer.length-1]) , is(equalTo(getMatrix(m1, riter, citer))));
    assertThat(getMatrix(m1, riter, citer[0], citer[citer.length-1]) , is(equalTo(getMatrix(m1, riter, citer))));
    assertThat(getMatrix(m1, riter[0], riter[riter.length-1], citer) , is(equalTo(getMatrix(m1, riter, citer))));
   

    // test getCol and getRow 
    // we assume transpose to be correct
    for(int i = 0; i < TESTMATRIX.length; i++) {
      double[] v = TESTMATRIX[i];
      assertArrayEquals(v, getRow(TESTMATRIX, i), 0.);
      assertArrayEquals(v, getCol(transpose(TESTMATRIX), i), 0.);
    }

  }
  
  /**
   * Testing the getRowDimensionality and getColumnDimensionality methods of VMath class.
   */
  @Test
  public void testGetDimensionality() {
    // FIXME: andom testcases
    final double[][] m3 = {
        {0,0,0,0,0},
        {0,0,0,0,0},
        {0,0,0,0,0} };
    
    assertEquals(3, getRowDimensionality(m3));
    assertEquals(3, getColumnDimensionality(transpose(m3) ));

    assertEquals(5, getColumnDimensionality(m3));
    assertEquals(5, getRowDimensionality(transpose(m3) ));
    
  }
  
  /**
   * Testing the diagonal and getdiagonal methods of VMath class.
   */
  @Test
  public void testDiagonal() {
    
    final double[] m = VMathVectorTest.TESTVEC;
    
    final double[][] m_diag = {{m[0],0,0,0,0},
                               {0,m[1],0,0,0},
                               {0,0,m[2],0,0},
                               {0,0,0,m[3],0},
                               {0,0,0,0,m[4]}};
    assertThat(diagonal(m), is(equalTo(m_diag)));
    
    final double[] dia_TEST = { TESTMATRIX[0][0],TESTMATRIX[1][1],TESTMATRIX[2][2],TESTMATRIX[3][3] };
    assertArrayEquals(dia_TEST, getDiagonal(TESTMATRIX), 0.);
    
    // if diagonal    is correct this is a test for getDiagonal
    // if getDiagonal is correct         a test for diagonal
    final double[] dia = { -2, 0, 1.21, 4, 7};
    assertArrayEquals(dia, getDiagonal(diagonal(dia)), 0.);
  }
  
  /**
   * Testing setMatrix, setCol and setRow methods of VMath class. 
   * 
   * Since the get-methods of the VMath class are tested independently of set in {@link #testGet()},
   * we mainly test here via  those get Methods.
   */
  @Test
  public void testSet() {

    // Initialize testmatrix
    final double[][] m1 = new double[20][25];
    
    // test setMatrix(Matrix, rows, columns, tosetMatrix)
    // column an row indexes. Assert that you don't address the same column/row twice  TODO: question assert not same colum row in index see below?
    final int[] row_index = {3, 5,10, 2,11,19, 8};
    final int[] col_index = {0,10, 7,19, 3, 6, 23, 5,4};
    // inputmatix with dimesions  7x9
    final double[][] sub1 = {
        {  0,  1,  2,  3,  4,  5,  6,  7,  8},
        { 10, 11, 12, 13, 14, 15, 16, 17, 18},
        { 20, 21, 22, 23, 24, 25, 26, 27, 28},
        { 30, 31, 32, 33, 34, 35, 36, 37, 38},
        { 40, 41, 42, 43, 44, 45, 46, 47, 48},
        { 50, 51, 52, 53, 54, 55, 56, 57, 58},
        { 60, 61, 62, 63, 64, 65, 66, 67, 68} };
    setMatrix(m1, row_index, col_index, sub1);
    assertThat(getMatrix(m1, row_index, col_index), is(equalTo(sub1)));
    
    // test setMatrix(Matrix, rowstart, rowend, columns, tosetMatrix)
    // testmatix with dimesions  5x9
    final double[][] sub2 = {        
        {  0,  1,  2,  3,  4,  5,  6,  7,  8},
        { 10, 11, 12, 13, 14, 15, 16, 17, 18},
        { 20, 21, 22, 23, 24, 25, 26, 27, 28},
        { 30, 31, 32, 33, 34, 35, 36, 37, 38},
        { 40, 41, 42, 43, 44, 45, 46, 47, 48}, };
    setMatrix(m1, 4, 8, col_index, sub2);
    assertThat(getMatrix(m1, 4, 8, col_index), is(equalTo(sub2)));
    
    // clear matrix for next test
    clear(m1);
    
    // test setMatrix(Matrix, rows, colstart, colend, tosetMatrix)
    // testmatix with dimesions  7x15
    final double[][] sub3 = {
        {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  10,  11,  12,  13,  14},
        { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 110, 111, 112, 113, 114},
        { 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 210, 211, 212, 213, 214},
        { 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 310, 311, 312, 313, 314},
        { 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 410, 411, 412, 413, 414},
        { 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 510, 511, 512, 513, 514},
        { 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 610, 611, 612, 613, 614}, };
    
    setMatrix(m1, row_index, 2, 16, sub3);
    assertThat(getMatrix(m1, row_index, 2, 16), is(equalTo(sub3)));

    // test setMatrix(Matrix, rowstart, rowend, colstart, colend, tosetMatrix)
    // testmatix with dimesions  7x3
    final double[][] sub4 = {
        {  0,  1,  2},
        { 10, 11, 12},
        { 20, 21, 22},
        { 30, 31, 32},
        { 40, 41, 42},
        { 50, 51, 52},
        { 60, 61, 62}, };
    
    setMatrix(m1, 0, 6, 16, 18, sub4);
    assertThat(getMatrix(m1, 0, 6, 16, 18), is(equalTo(sub4)));
    
    // check that setting a full matrix  FIXME: maybe randomize
    final double[][] m2 = TESTMATRIX;
    final double[][] res1 = new double[getRowDimensionality(m2)][getColumnDimensionality(m2)]; 
    setMatrix(res1, 0, getRowDimensionality(m2)-1, 0, getColumnDimensionality(m2)-1, m2);
    assertThat(res1, is(equalTo(m2)));
    
    // check that different the different setMatrix methods to the same if exchangeable
    final int[] riter = {0,1,2,3}, citer = {0,1,2,3,4};
    final double[][] res2 = new double[riter.length][citer.length];
    
    setMatrix(res2, riter[0], riter[riter.length-1], citer[0], citer[citer.length-1], m2);
    assertThat(res2, is(equalTo( getMatrix(m2, riter, citer) ))); 
    clear(res2);
    
    setMatrix(res2, riter, citer[0], citer[citer.length-1], m2);
    assertThat(res2, is(equalTo( getMatrix(m2, riter, citer) ))); 
    clear(res2);
    
    setMatrix(res2, riter[0], riter[riter.length-1], citer, m2);
    assertThat(res2, is(equalTo( getMatrix(m2, riter, citer) ))); 
    
    
    // testing setCol and setRow
    final double[] col = VMathVectorTest.TESTVEC;
    final double[] row = VMathVectorTest.TESTVEC;
    
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
   * Testing the _solve method of VMath class.
   */
  @Test
  public void testSolve() {
    // TODO: implement solve(A, B);
  }
  
  /**
   * Testing the appendColums method of VMath class.
   * 
   * FIXME: Question maybe better name appendMatrix? or document to complicated?
   */
  @Test
  public void testAppendColums() {
    
    final double[][] m1 = {
        {1,2,3},
        {1,2,3},
        {1,2,3} };
    
    final double[][] m2 = {
        {4,5,6,7},
        {4,5,6,7},
        {4,5,6,7} };

    final double[][] m1_res = {
        {m1[0][0], m1[0][1], m1[0][2],   m2[0][0], m2[0][1], m2[0][2], m2[0][3]},
        {m1[1][0], m1[1][1], m1[1][2],   m2[1][0], m2[1][1], m2[1][2], m2[1][3]},
        {m1[2][0], m1[2][1], m1[2][2],   m2[2][0], m2[2][1], m2[2][2], m2[2][3]},
    };
    
    assertThat(appendColumns(m1, m2), is(equalTo(m1_res)));
    // TODO which reference wanted?
  }
  // TODO: Question test void methods to stay void?
  
  /**
   * Testing the project(vector, Matrix) method of VMath class.
   */
  @Test
  public void testProject() {
    final double[][] m1 = {
        {1, 0},
        {0, 1},
        {0, 0},};
    
    // testvector v1 is already in subspace
    final double[] v1 = {-8,9,0};
    assertThat(project(v1, m1), is(equalTo(v1)));
    
    // testvector v2 is orthogonal to subspace
    final double[] v2 = {0,0,-1};
    final double[] pv2= {0,0,0};
    assertThat(project(v2, m1), is(equalTo(pv2)));
    
    // third testcase FIXME: question more difficult example needed?
    final double[] v3 =  {4,  3, 13};
    final double[] pv3 = {4,  3, 0};
    assertThat(project(v3, m1), is(equalTo(pv3)));
    
  }

  /**
   * Testing the inverse method of VMath class.
   */
  @Test
  public void testInverse() {
    // TODO: implement inverse(elements)
  }
  
  // TODO: implement Dimension mismatch
  
  /**
   * Testing that *Equals Matrix-operations of the {@link VMath} 
   * class work in place and testing that other matrix-operations
   * create a new instance.
   * 
   * Tests of matrix-methods where the class of the instance 
   * returned differs form class of input method are reasonably omitted,
   * when testing reference.
   * <p>
   * For a complete list of tested methods: <br>
   */
  @Test
  public void testReference() {
    final double[][] m1 = {{1}}, m2 = {{1}};
    
    // testing the methods as in testAppendColums
    assertNotSame(m1, appendColumns(m1, m2));
    
    // testing the methods as in Diagonal omitted
    
    // testing the methods as in testGet TODO: question inner object reference possible
    
    // testing the methods as in omitted
    
    // testing the methods as in testInverse
    assertNotSame(m1, inverse(m1));
    
    // testing the methods as in testMatrixMatrixMultiplication
    assertNotSame(m1, times(m1,m2));
    assertNotSame(m1, times(m1,m2));
    
    assertNotSame(m1, transposeTimesTranspose(m1, m2));
    assertNotSame(m2, transposeTimesTranspose(m1, m2));
    
    assertNotSame(m1, timesTranspose(m1,m2));
    assertNotSame(m2, timesTranspose(m1,m2));
    
    assertNotSame(m1, transposeTimes(m1,m2));
    assertNotSame(m2, transposeTimes(m1,m2));
    
    // testing the methods as in testMatrixScalarMultiplication 
    final double s1 = 1;
    assertNotSame(m1, times(m1, s1));
    assertSame(m1, timesEquals(m1, s1));
    
    // testing the methods as in testTranspose
    assertNotSame(m1, transpose(m1));
    
    // testing the methods as in testMatrixVectorMultiplication
    final double[] v1 = {1}, v2 = {1};
    
    assertNotSame(m2, times(v1, m2));
    assertNotSame(m1, times(m1, v2));
    
    assertNotSame(m1, transposeTimes(m1, v2));
    assertNotSame(m2, transposeTimes(v1, m2));
    
    assertNotSame(m2, timesTranspose(v1, m2));
    
    // testing the methods as in testMinus
    // TODO: implement when Minus
    // testing the methods as in testPlus
    // TODO: implement when Plus
    // testing the methods as in testSet omitted no return values
    // testing the methods as in testSolve omitted
    // testing the methods as in testUnitMatrix omitted
  }

}

// TODO: Have a look a normalize options of VMathclass
