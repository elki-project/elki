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
   * considerations we don't want to guarantee for an exception. So in this case we can't decide 
   * if a dimension mismatch has occurred. This is why we need the {@link failWrapper} method. <p>
   * Let's make an example of usage: <br> 
   * Let's take two Vectors v1, v2 with  different lengths. 
   * So v1 + v2 should not be possible, so we want v1.length to equal v2.length and assert with the 
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
    
    final double[][] m2 = {{0, 1, 0.123451234512345, 2},
                           {2, 3, 4.123451234512345,-1}};
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
   * Testing the hashcode() method of VMath class.
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
   * Testing the Equals methods on vectors an matrixes of VMath class.
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
        { 1, 2, 3},
        { 7, 3, 9},
        { 0, 2, 1},
        {20,-5, 4},
        {-3, 0, 3},
        { 1, 1, 1}};
    
    // make copy of by hand to be independent of copy module
    final double[][] m1_copy = {
        { 1, 2, 3},
        { 7, 3, 9},
        { 0, 2, 1},
        {20,-5, 4},
        {-3, 0, 3},
        { 1, 1, 1}};
        
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

    final double[][] m = copy(VMathMatrixTest.TESTMATRIX);
    
    // TODO: make copy by hand to be independent of copy. Update structure.
    
    // basic function test
    assertTrue(almostEquals(m , VMathMatrixTest.TESTMATRIX));
    assertTrue(almostEquals(m , VMathMatrixTest.TESTMATRIX, 0.));
    assertFalse(almostEquals(m, identity(4, 5)));
    assertFalse(almostEquals(m, identity(4, 5), EPSILON));
    
    
    // fail if dimensions mismatch
    assertFalse(almostEquals(m, new double[][] {{1}}));
    
    // fail if difference d > maxdelta with increasing maxdelta
    // maxdelta = EPSILON
    double[][] res_diff = copy(VMathMatrixTest.TESTMATRIX); res_diff[3][3] += 1.5*EPSILON;
    assertFalse(almostEquals(m, res_diff, EPSILON));
    res_diff[3][3] -= EPSILON;
    assertTrue(almostEquals(m, res_diff, EPSILON));
    
    // maxdelta DELTA of VMath respectively 1E-5
    res_diff[0][4] += 1.5E-5;
    assertFalse(almostEquals(m, res_diff));
    res_diff[0][4] -= 1E-5;
    assertTrue(almostEquals(m, res_diff));
    
    // maxdelta = EPSILON
    res_diff[2][1] = 1E20; res_diff[2][1] += 1.5E9;
    assertFalse(almostEquals(m, res_diff, 1E10));
    res_diff[2][1] -= 1E9;
    assertTrue(almostEquals(m, res_diff, 1E10));
    // TODO QUEST: What is this case getClass() for? why is this test failing?
    
  }

}
// TODO put testGet testSet here, getDimensionality, testDiagonal
// TODO put testUniMatrix testUnitvector here
