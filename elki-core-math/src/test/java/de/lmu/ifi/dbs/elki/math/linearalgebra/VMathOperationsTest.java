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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test the VMath class methods which are non mathematical operations on vectors or matrixes.
 *
 * @author Merlin Dietrich
 * 
 */
public final class VMathOperationsTest {
    
  /**
   * A small number to handle numbers near 0 as 0.
   */
  protected static final double EPSILON = 1E-15;
  
  /**
   * Method for testing that exceptions are raised if dimension mismatches in input data occur.
   *    
   * This test caches Assertion Errors with a given Message if the -ea option is set or rather if assertions are executed. 
   * If the -ea option is not set, an ArrayIndexOutOfBoundsException may occur, but out of optimization 
   * considerations we don't want to guarantee this exception. So in this case we can't decide 
   * if a dimension mismatch has occurred. This is why we need the {@link failWrapper} method. <p>
   * Let's make an example of usage: <br> 
   * Let's take two Vectors v1, v2 with  different lengths. 
   * So v1 + v2 should not be possible to compute, so we want v1.length to equal v2.length and assert with the 
   * {@link VMath#ERR_VEC_DIMENSIONS} error Message. If we think of any implementation of a plus(v1, v2) 
   * method with vectors as arrays e.g. {@link VMath#plus(double[], double[])}, 
   * we are going to iterate either over the length of v1 or v2. But with assertions turned of (no -ea set)
   * either v1+v2 or v2+v1 is  going to raise an ArrayIndexOutOfBoundsException,
   * while the other is not. 
   * <pre>
   * assertDimensionMismatch( {@link VMathVectorTest#ERR_VEC_DIMENSIONS},() -> plus(v1, v2) )
   * </pre>
   * 
   * @param msg Assertion Message to be raised with -ea on
   * @param r runnable of the method to be tested
   */
  protected static void assertDimensionMismatch(String msg, Runnable r) {
    try {
      r.run();
    } catch(AssertionError e) {
      assertEquals(msg, e.getMessage());
      
      return; // If assertions are enabled.
    } catch(ArrayIndexOutOfBoundsException e) {
      return; // Expected failure
    }
    // We only guarantee an error if assertions are enabled with -ea. If they are not the next line will do nothing.
    assert(failWrapper("Failed to raise expected Assertationerrormessage".concat(msg)));
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
    fail(msg);
    return true;
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
    final double[] v1 = {1,2,3,4};
    final double[] v1_res = copy(v1);
    assertArrayEquals(v1, v1_res,0.);
    assertNotSame(v1, v1_res);
    
    final double[] v2 = VMathVectorTest.TESTVEC;
    final double[] v2_res = copy(v2);
    assertArrayEquals(v2, v2_res,0.);
    assertNotSame(v2, v2_res);
    
    //testing copy(Matrix) method
    final double[][] m1 = VMathMatrixTest.TESTMATRIX;
    final double[][] m1_copy = copy(m1);
    assertThat(m1, is(equalTo(m1_copy)));
    assertNotSame(m1, m1_copy);
    
    final double[][] m2 = {{0, 1, 0.123451234512345, 2}, //
                           {2, 3, 4.123451234512345,-1}, //
    };
    final double[][] m2_res = copy(m2);
    assertThat(m2, is(equalTo(m2_res)));
    assertNotSame(m2, m2_res);
    
    //testing columPackedCopy(Matrix) method
    final double[] m2_colpack = {0,2,  1,3,  0.123451234512345,4.123451234512345,  2,-1};
    
    final double[] m2_colpackres = columnPackedCopy(m2);
    assertArrayEquals(columnPackedCopy(m2), m2_colpack, 0.);
    assertNotSame(m2, m2_colpackres);
    
    //testing rowPackedCopy(Matrix) method
    final double[] m2_rowpack = {0,1,0.123451234512345,2,  2,3,4.123451234512345,-1};
    final double[] m2_rowpackres = rowPackedCopy(m2);
    assertArrayEquals(rowPackedCopy(m2), m2_rowpack, 0.);
    assertNotSame(m2, m2_rowpackres);   
  }
  

  /**
   * Testing the hashcode() methods of VMath class.
   */
  @Test
  public void testHashcode() {
    
    final double[] v = VMathVectorTest.TESTVEC;
    final double[][] m = VMathMatrixTest.TESTMATRIX;
    assertEquals(Arrays.hashCode(v), VMath.hashCode(v), 0.);
    assertEquals(Arrays.deepHashCode(m), VMath.hashCode(m), 0.);
  }
  
  /**
   * Testing the clear(vector), clear(matrix) methods of VMath class.
   */
  @Test
  public void testClear() {
    
    // test clear(vector) 
    final double[] v = copy(VMathVectorTest.TESTVEC); clear(v); 
    final double[] zeros5 = {0,0,0,0,0};
    assertArrayEquals(zeros5, v, 0.);
    
    // test clear(matrix) 
    final double[][] m = copy(VMathMatrixTest.TESTMATRIX); clear(m);   
    final double[][] zeros4x5 = {
        {0,0,0,0,0}, //
        {0,0,0,0,0}, //
        {0,0,0,0,0}, //
        {0,0,0,0,0}, //
    };
    
    assertThat(zeros4x5, is(equalTo(m)));
  }

  /**
   * Testing the equals methods of VMath class, which compare vectors or matrixes.
   */
  @Test
  public void testEquals() {
    // equals(Vector)
    final double[] v1 = {2,4,3,0,-5,9};
    // copy made by hand to be independent of copy module
    final double[] v1_copy = {2,4,3,0,-5,9};
    
    assertThat(v1, is(equalTo(v1)));
    assertThat(v1, is(equalTo(v1_copy)));
    assertThat(unitVector(6, 2), is(not(equalTo(v1))));
    
    
    // equals(Matrix)
    final double[][] m1 = {
        { 1, 2, 3}, //
        { 7, 3, 9}, //
        { 0, 2, 1}, //
        {20,-5, 4}, //
        {-3, 0, 3}, //
        { 1, 1, 1}, //
    };
    
    // make copy of by hand to be independent of copy module
    final double[][] m1_copy = {
        { 1, 2, 3}, //
        { 7, 3, 9}, //
        { 0, 2, 1}, //
        {20,-5, 4}, //
        {-3, 0, 3}, //
        { 1, 1, 1}, //
    };
        
    assertThat(m1, is(equalTo(m1)));
    assertThat(m1, is(equalTo(m1_copy)));
    assertThat(identity(6, 3), is(not(equalTo(m1))));
  }
  
  /**
   * Testing the almostEquals methods of VMath class.
   * 
   * Note that almostEquals(m1,m2) is equivalent to almostEquals(m1,m2, {@link VMath#DELTA})
   */
  @Test
  public void testMatrixAlmosteq() {

    final double[][] m1 = copy(VMathMatrixTest.TESTMATRIX);
    final double[][] m2 = copy(VMathMatrixTest.TESTMATRIX);

    // TODO: make copy by hand to be independent of copy. Update structure.
    
    // basic function test
    assertTrue(almostEquals(m1 , m2));
    assertTrue(almostEquals(m1 , m2, 0.));
    assertFalse(almostEquals(m1, identity(4, 5)));
    assertFalse(almostEquals(m1, identity(4, 5), EPSILON));
    
    // fail if dimensions mismatch
    assertFalse(almostEquals(m1, new double[][] {{1}}));
    
    // testing that assert fails if difference d > maxdelta else not
    // we test with increasing maxdelta and work on the same data
    // maxdelta = EPSILON
    double[][] res_diff = copy(m1); res_diff[3][3] += 1.5*EPSILON;
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
  /*
   * ----------- testing initialization get and set methods ----------------
   */ 
  
  /**
   * Testing the {@link VMath#unitVector(int, int)} method.
   */
  @Test
  public void testUnitVector() { 
    // test if unitVector is returned
    // for dimension 3
    assertArrayEquals(new double[] {1,0,0}, unitVector(3,0),0. );
    assertArrayEquals(new double[] {0,1,0}, unitVector(3,1),0. );
    assertArrayEquals(new double[] {0,0,1}, unitVector(3,2),0. );
    
    // for dimension 5
    assertArrayEquals(new double[] {1,0,0,0,0}, unitVector(5,0),0. );
    assertArrayEquals(new double[] {0,1,0,0,0}, unitVector(5,1),0. );
    assertArrayEquals(new double[] {0,0,1,0,0}, unitVector(5,2),0. );
    assertArrayEquals(new double[] {0,0,0,1,0}, unitVector(5,3),0. );
    assertArrayEquals(new double[] {0,0,0,0,1}, unitVector(5,4),0. );
   
  }

  /**
   * Testing the unitMatrix and the identity method of VMath class.
   */
  @Test
  public void testUnitMatrixAndIdentity() {
    // test unitMatrix(dim) and unitMatrix(dim) equals identity(dim, dim)
    final double[][] m_unit = {{1,0,0,0,0}, //
                               {0,1,0,0,0}, //
                               {0,0,1,0,0}, //
                               {0,0,0,1,0}, //
                               {0,0,0,0,1}, //
    };

    assertThat(unitMatrix(5), is(equalTo(m_unit)));
    assertThat(identity(5, 5), is(equalTo(m_unit)));
 
    // test identity with dimensions 3x5 and  5x3
    final double[][] m_identity3x5 = {{1,0,0,0,0}, //
                                      {0,1,0,0,0}, //
                                      {0,0,1,0,0}, //
    };

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
       {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9}, //
       { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, //
       { 20, 21, 22, 23, 24, 25, 26, 27, 28, 29}, //
       { 30, 31, 32, 33, 34, 35, 36, 37, 38, 39}, //
       { 40, 41, 42, 43, 44, 45, 46, 47, 48, 49}, //
       { 50, 51, 52, 53, 54, 55, 56, 57, 58, 59}, //
       { 60, 61, 62, 63, 64, 65, 66, 67, 68, 69}, //
   };
  
   // array of row indices
   final int[] r = {0,3,5};
   // array of column indices
   final int[] c = {1,3,3,9};
   
   // test getMatrix(Matrix, rows, columns)
   final double[][] sub1 = {
       {m1[r[0]][c[0]], m1[r[0]][c[1]], m1[r[0]][c[2]], m1[r[0]][c[3]]}, //
       {m1[r[1]][c[0]], m1[r[1]][c[1]], m1[r[1]][c[2]], m1[r[1]][c[3]]}, //
       {m1[r[2]][c[0]], m1[r[2]][c[1]], m1[r[2]][c[2]], m1[r[2]][c[3]]}, //
   };
  
   assertThat(getMatrix(m1, r, c) , is(equalTo(sub1))); 
   
   // test getMatrix(Matrix, rowstart, rowend , columns)
   final double[][] sub2 = {
       {m1[2][c[0]], m1[2][c[1]], m1[2][c[2]], m1[2][c[3]]}, //
       {m1[3][c[0]], m1[3][c[1]], m1[3][c[2]], m1[3][c[3]]}, //
       {m1[4][c[0]], m1[4][c[1]], m1[4][c[2]], m1[4][c[3]]}, //
       {m1[5][c[0]], m1[5][c[1]], m1[5][c[2]], m1[5][c[3]]}, //
   };
   
   assertThat(getMatrix(m1, 2, 5, c) , is(equalTo(sub2)));
   
   // test getMatrix(Matrix, rows, columnstart, columnend)
   final double[][] sub3 = {
       {m1[r[0]][4], m1[r[0]][5], m1[r[0]][6], m1[r[0]][7]}, //
       {m1[r[1]][4], m1[r[1]][5], m1[r[1]][6], m1[r[1]][7]}, //
       {m1[r[2]][4], m1[r[2]][5], m1[r[2]][6], m1[r[2]][7]}, //
   };
  
   assertThat(getMatrix(m1, r, 4, 7) , is(equalTo(sub3)));
   
   // test getMatrix(Matrix, rowstart, rowend, columnstart, columnend)
   final double[][] sub4 = {
   {m1[0][6], m1[0][7], m1[0][8]}, //
   {m1[1][6], m1[1][7], m1[1][8]}, //
   {m1[2][6], m1[2][7], m1[2][8]}, //
   };
    
   assertThat(getMatrix(m1, 0, 2, 6, 8) , is(equalTo(sub4)));
   
   // cross methods and general testing
   assertThat(getMatrix(m1, 0, getRowDimensionality(m1)-1, 0, getColumnDimensionality(m1)-1) , is(equalTo(m1)));
  
   final int[] riter = {3,4,5};
   final int[] citer = {0,1,2};
   assertThat(getMatrix(m1, riter[0], riter[riter.length-1], citer[0], citer[citer.length-1]) , is(equalTo(getMatrix(m1, riter, citer))));
   assertThat(getMatrix(m1, riter, citer[0], citer[citer.length-1]) , is(equalTo(getMatrix(m1, riter, citer))));
   assertThat(getMatrix(m1, riter[0], riter[riter.length-1], citer) , is(equalTo(getMatrix(m1, riter, citer))));
  
   // test getCol and getRow 
   // we assume transpose to be correct
   for(int i = 0; i < VMathMatrixTest.TESTMATRIX.length; i++) {
     double[] v = VMathMatrixTest.TESTMATRIX[i];
     assertArrayEquals(v, getRow(VMathMatrixTest.TESTMATRIX, i), 0.);
     assertArrayEquals(v, getCol(transpose(VMathMatrixTest.TESTMATRIX), i), 0.);
   }
  
  }
  
  /**
  * Testing the getRowDimensionality and getColumnDimensionality methods of VMath class.
  */
  @Test
  public void testGetDimensionality() {
   final double[][] m3 = {
       {0,0,0,0,0}, //
       {0,0,0,0,0}, //
       {0,0,0,0,0}, //
   };
   
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
   
   final double[][] m_diag = {{m[0],0,0,0,0}, //
                              {0,m[1],0,0,0}, //
                              {0,0,m[2],0,0}, //
                              {0,0,0,m[3],0}, //
                              {0,0,0,0,m[4]}, //
   };
   assertThat(diagonal(m), is(equalTo(m_diag)));
   
   final double[] dia_TEST = { VMathMatrixTest.TESTMATRIX[0][0],VMathMatrixTest.TESTMATRIX[1][1],VMathMatrixTest.TESTMATRIX[2][2],VMathMatrixTest.TESTMATRIX[3][3] };
   assertArrayEquals(dia_TEST, getDiagonal(VMathMatrixTest.TESTMATRIX), 0.);
   
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
   // column an row indexes. Assert that you don't address the same column/row twice.
   final int[] row_index = {3, 5,10, 2,11,19, 8};
   final int[] col_index = {0,10, 7,19, 3, 6, 23, 5,4};
   // inputmatix with dimensions  7x9
   final double[][] sub1 = {
       {  0,  1,  2,  3,  4,  5,  6,  7,  8}, //
       { 10, 11, 12, 13, 14, 15, 16, 17, 18}, //
       { 20, 21, 22, 23, 24, 25, 26, 27, 28}, //
       { 30, 31, 32, 33, 34, 35, 36, 37, 38}, //
       { 40, 41, 42, 43, 44, 45, 46, 47, 48}, //
       { 50, 51, 52, 53, 54, 55, 56, 57, 58}, //
       { 60, 61, 62, 63, 64, 65, 66, 67, 68}, //
   };
   setMatrix(m1, row_index, col_index, sub1);
   assertThat(getMatrix(m1, row_index, col_index), is(equalTo(sub1)));
   
   // test setMatrix(Matrix, rowstart, rowend, columns, tosetMatrix)
   // testmatix with dimensions  5x9
   final double[][] sub2 = {        
       {  0,  1,  2,  3,  4,  5,  6,  7,  8}, //
       { 10, 11, 12, 13, 14, 15, 16, 17, 18}, //
       { 20, 21, 22, 23, 24, 25, 26, 27, 28}, //
       { 30, 31, 32, 33, 34, 35, 36, 37, 38}, //
       { 40, 41, 42, 43, 44, 45, 46, 47, 48}, //
   };
   setMatrix(m1, 4, 8, col_index, sub2);
   assertThat(getMatrix(m1, 4, 8, col_index), is(equalTo(sub2)));
   
   // clear matrix for next test
   clear(m1);
   
   // test setMatrix(Matrix, rows, colstart, colend, tosetMatrix)
   // testmatix with dimensions  7x15
   final double[][] sub3 = {
       {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  10,  11,  12,  13,  14}, //
       { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 110, 111, 112, 113, 114}, //
       { 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 210, 211, 212, 213, 214}, //
       { 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 310, 311, 312, 313, 314}, //
       { 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 410, 411, 412, 413, 414}, //
       { 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 510, 511, 512, 513, 514}, //
       { 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 610, 611, 612, 613, 614}, //
   };
   
   setMatrix(m1, row_index, 2, 16, sub3);
   assertThat(getMatrix(m1, row_index, 2, 16), is(equalTo(sub3)));
  
   // test setMatrix(Matrix, rowstart, rowend, colstart, colend, tosetMatrix)
   // testmatix with dimensions  7x3
   final double[][] sub4 = {
       {  0,  1,  2}, //
       { 10, 11, 12}, //
       { 20, 21, 22}, //
       { 30, 31, 32}, //
       { 40, 41, 42}, //
       { 50, 51, 52}, //
       { 60, 61, 62}, //
   };
   
   setMatrix(m1, 0, 6, 16, 18, sub4);
   assertThat(getMatrix(m1, 0, 6, 16, 18), is(equalTo(sub4)));
   
   // check that setting a full matrix
   final double[][] m2 = VMathMatrixTest.TESTMATRIX;
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
  * Testing the appendColums method of VMath class.
  */
  @Test
  public void testAppendColums() {
   
   final double[][] m1 = {
       {1,2,3}, //
       {1,2,3}, //
       {1,2,3}, //
   };
   
   final double[][] m2 = {
       {4,5,6,7}, //
       {4,5,6,7}, //
       {4,5,6,7}, //
   };
  
   final double[][] m1_res = {
       {m1[0][0], m1[0][1], m1[0][2],   m2[0][0], m2[0][1], m2[0][2], m2[0][3]}, //
       {m1[1][0], m1[1][1], m1[1][2],   m2[1][0], m2[1][1], m2[1][2], m2[1][3]}, //
       {m1[2][0], m1[2][1], m1[2][2],   m2[2][0], m2[2][1], m2[2][2], m2[2][3]}, //
   };
   
   assertThat(appendColumns(m1, m2), is(equalTo(m1_res)));
  }
  
  /**
   * Testing that the *Equals methods tested in this class work in place and that the other
   * methods tested create a new instance.
  * 
  * Tests of methods where the class of the instance returned differs form the class of input are reasonably omitted,
  * when testing reference. We omit the copy methods as well because the only testing made there is the reference.
  */
  @Test
  public void testReference() {

    final double[][] m1 = {{1}};

    // testing the appendColums method as it is now: not working in place
    assertNotSame(m1, appendColumns(m1, m1));
    
    // testing that methods as in testGet return new copys of the submatrix to get
    final double[] inner = {1};
    final double[][] m2 = {inner};
    // assert that inner reference is possible
    assertSame(inner, m2[0]);
    
    final int[] rows = {0}, cols = {0};
    final int c0 = 0, c1 = 0, r0 = 0, r1 = 0;

    assertNotSame(inner, getMatrix(m2, rows, cols)[0]);
    assertNotSame(inner, getMatrix(m2, r0, r1, cols)[0]);
    assertNotSame(inner, getMatrix(m2, rows, c0, c1)[0]);
    assertNotSame(inner, getMatrix(m2, r0, r1, c0, c1)[0]);
    
    assertNotSame(inner, getRow(m2, r0));

    // testing that the methods as in testSet, set the rows in the submatrix to refer to a new instance.
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
   * Testing that correct Error is raised when dimension of the input data mismatch the needs of the method.
   * Methods where no error is to be raised are omitted. 
   * <p>
   * See {@link assertDimensionMismatch} for details.
   */
  @Test
  public void testDimensionMismatch() {
    
    // unitVector only use index out of Bound exception in assertDimensionMismatch
    assertDimensionMismatch("", () -> unitVector(0,0));
    assertDimensionMismatch("", () -> unitVector(4,4));
    assertDimensionMismatch("", () -> unitVector(10,15));

    // testing the methods as in testGet 
    final int[] r = {5}, c = {5};
    final int r1 = 5, c1 = 5;
    assertDimensionMismatch("", () -> getMatrix(unitMatrix(2), r, c ));
    assertDimensionMismatch("", () -> getMatrix(unitMatrix(2), 0, r1, c));
    assertDimensionMismatch("", () -> getMatrix(unitMatrix(2), r, 0, c1));
    assertDimensionMismatch("", () -> getMatrix(unitMatrix(2), 0, r1, 0, c1));
    assertDimensionMismatch("", () -> getCol(unitMatrix(2), c1));
    assertDimensionMismatch("", () -> getRow(unitMatrix(2), r1));
        
    // testing the methods as in testSet
    assertDimensionMismatch("", () -> setMatrix(unitMatrix(2), r, c, unitMatrix(6) ));
    assertDimensionMismatch("", () -> setMatrix(unitMatrix(2), 0, r1, c, unitMatrix(6)));
    assertDimensionMismatch("", () -> setMatrix(unitMatrix(2), r, 0, c1, unitMatrix(6)));
    assertDimensionMismatch("", () -> setMatrix(unitMatrix(2), 0, r1, 0, c1, unitMatrix(6)));
    assertDimensionMismatch("Dimensionalities do not agree.", () -> setCol(unitMatrix(2), c1, unitVector(6,0)));
    assertDimensionMismatch("Dimensionalities do not agree.", () -> setRow(unitMatrix(2), r1, unitVector(6,0)));

  }

  
  }
