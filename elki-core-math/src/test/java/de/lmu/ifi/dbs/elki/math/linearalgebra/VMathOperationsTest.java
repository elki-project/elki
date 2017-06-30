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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test the VMath class methods which are non mathimatical operations on Vector or Matrixes.
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
   * Test the copy functions of VMath class.
   * 
   * Tested Methods:
   * copy(vector), TOIMPL: copy(Matrix), columPackedCopy(Matrix), rowPackedcCopy(Matrix)
   */
  @Test
  public void testCopy() {
    
    // testing copy(vector) method
    double[] v; double[] v_copy;
    
    v   = new double[] {1,2,3,4};
    v_copy = copy(v);
    assertArrayEquals(v, v_copy,0.);
    assertNotSame(v, v_copy);
    
    v = new double[] {0};
    v_copy = copy(v);
    assertArrayEquals(v, v_copy,0.);
    assertNotSame(v, v_copy);
    
    //testing copy(Matrix) method
    final double[][] m1 = VMathMatrixTest.TESTMATRIX;
    final double[][] m1_copy = copy(m1);
    assertTrue(Equals(m1, m1_copy));
    assertNotSame(m1, m1_copy);
    
    final double[][] m2 = {{0, 1, 0.123451234512345, 2},
                           {2, 3, 4.123451234512345,-1}};
    final double[][] m2_copy = copy(m2);
    assertTrue(Equals(m2, m2_copy));
    assertNotSame(m2, m2_copy);
    
    //testing columPackedCopy(Matrix) method
    final double[] m2_colpack = {0,2,  1,3,  0.123451234512345,4.123451234512345,  2,-1};
    
    final double[] m2_colpackres = columnPackedCopy(m2);
    assertArrayEquals(columnPackedCopy(m2), m2_colpack, 0.);
    assertNotSame(m2_colpack, m2_colpackres);
    
    //QUEST: row packed = colum packed vis versa?
    final double[] m2_rowpack = {0,1,0.123451234512345,2,  2,3,4.123451234512345,-1};
    final double[] m2_rowpackres = rowPackedCopy(m2);
    assertArrayEquals(rowPackedCopy(m2), m2_rowpack, 0.);
    assertNotSame(m2_rowpack, m2_rowpackres);
    
    
  }
  

  /**
   * Testing the hashcode() method of VMath class.
   */
  @Test
  public void testHashcode() {

    // TODO QUEST: hashCode(v1) hashCode(m1) test needed?
  }
  
  /**
   * Testing the clear(vector), clear(vector) methods of VMath class.
   * 
   * QUEST vector and matrix in one mehtod
   */
  @Test
  public void testClear() {
    
    // test clear(vector) with TESTVEC of lenght 5
    double[] v = VMathVectorTest.TESTVEC; clear(v); 
    final double[] zeros5 = {0,0,0,0,0};
    assertArrayEquals(zeros5, v, 0.);
    
    // test clear(matrix) with TESTMATRIX of dimesions 45x5
    double[][] m = copy(VMathMatrixTest.TESTMATRIX); clear(m);   
    final double[][] zeros4x5 = {{0,0,0,0,0}, {0,0,0,0,0}, {0,0,0,0,0}, {0,0,0,0,0}};
    
    assertTrue(almostEquals(zeros4x5 , m, 0.));
  }

  /**
   * Testing the Equals methods on vectors an matrixes of VMath class.
   */
  @Test
  public void testEquals() {
    // equals(Matrix)
    final double[] v1 = {2,4,3,0,-5,9};
    
    // make copy of by hand to be independent of copy 
    // module to be able to use Equals in testcopy
    final double[] v1_copy = {2,4,3,0,-5,9};
    
    assertTrue(Equals(v1, v1));
    assertTrue(Equals(v1, v1_copy));
    assertFalse(Equals(unitVector(6, 2), v1));
    
    
    // Equals(Matrix)
    final double[][] m1 = {{ 1, 2, 3},
                           { 7, 3, 9},
                           { 0, 2, 1},
                           {20,-5, 4},
                           {-3, 0, 3},
                           { 1, 1, 1}};
    
    // make copy of by hand to be independent of copy 
    // module to be able to use Equals in testcopy
    final double[][] m1_copy = {{ 1, 2, 3},
                                { 7, 3, 9},
                                { 0, 2, 1},
                                {20,-5, 4},
                                {-3, 0, 3},
                                { 1, 1, 1}};
        
    assertTrue(Equals(m1, m1));
    assertTrue(Equals(m1, m1_copy));
    assertFalse(Equals(identity(6, 3), m1));
    
    // TODO: Question: more testcases?
    
  }
  
  /**
   * Testing the almostEquals and methods of VMath class.
   * 
   * Note that almostEquals(m1,m2) is equivalent to almostEquals(m1,m2, {@link VMath#DELTA})
   * @see VMath#almostEquals(double[][], double[][])
   */
  @Test
  public void testMatrixAlmosteq() {

    final double[][] m = copy(VMathMatrixTest.TESTMATRIX);
    
    final double[][] unit4x5 = {
        {1,0,0,0,0}, {0,1,0,0,0}, {0,0,1,0,0}, {0,0,0,1,0}
    };
    
    // basic function test
    assertTrue(almostEquals(m , VMathMatrixTest.TESTMATRIX));
    assertTrue(almostEquals(m , VMathMatrixTest.TESTMATRIX, 0.));
    assertFalse(almostEquals(m, unit4x5));
    assertFalse(almostEquals(m, unit4x5, EPSILON));
    
    
    // fail if dimensions mismatch
    assertFalse(almostEquals(m, VMathMatrixTest.DIMTESTMATRIX));
    
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
    res_diff[2][1] = 1E20; res_diff[2][1] += 1.5E10;
    assertFalse(almostEquals(m, res_diff, 1E10));
    res_diff[2][1] -= 1E10;
    assertTrue(almostEquals(m, res_diff, 1E10));
    // QUEST: What is this case getClass() for? why is this test failing?
    
  }

}