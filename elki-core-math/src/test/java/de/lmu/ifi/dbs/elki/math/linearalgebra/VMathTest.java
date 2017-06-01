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


import org.junit.Rule;
import org.junit.rules.ExpectedException;


import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import net.jafama.FastMath;

/**
 * Test the V(ector)Math class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */

public class VMathTest {
//private void checkVectorEquals(double[] original,double[] result) {
//assertTrue(original.length == result.length);
//for(int i = 0; i < result.length; i++)
//  assertEquals(original[i],result[i],0.);
//
//
//}
  /**
   * A small number to handle numbers near 0 as 0.
   */
  private static final double EPSILON = 1E-15;
  
  /**
   * Error message (in assertions!) when vector dimensionalities do not agree.
   */
  private static final String ERR_VEC_DIMENSIONS = "Vector dimensions do not agree.";
  
  /**
   * A collection of v1 testvectors for later use.
   */
  private static final double[][] V1_TESTVEC = { 
      {},
      {1,2,3},
      {4,5,6,0},
  };
  
  /**
   * Vector v1 as TestVector
   */
  private static double[] v1;
  
  /**
   * Vector v2 as TestVector
   */
  private static double[] v2;
  
  /**
   * Skalar s1 as TestSkalar
   */
  private static double s1;

  /**
   * Skalar s2 as TestSkalar
   */
  private static double s2;
  
  /**
   * Skalar d as TestSkalar
   */
  private static double d;
  
  /**
   * Vector RES_VEC as storage for results
   */
  private static double[] RES_VEC;

  @Rule
  public final ExpectedException exception = ExpectedException.none();
  
  @Test //sqrt implemented was missing at first. Case 0 not working jet
  public void testrandomNormalizedVector() {
    
    for(int i = 1; i < 10; i++){
      final double[] v1 = randomNormalizedVector(i);
      
      // test if vector is normalized, checks if squared sums equal 1
      double vnorm = 0;
      for(int j=0; j < i;j++) {
        final double x = v1[j];
        vnorm += x*x;
      };
      assertEquals(1,vnorm,EPSILON); 
      
      // possible to use VMath euclideanLength()
      // assertEquals(1,euclideanLength(v1),EPSILON) or
      // assertEquals(1,squareSum(v1),EPSILON)   
      
    };
    
  }
  

  @Test //index is given Starting 0. 
  public void testunitVector() {
    
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
    
    // Assertion that index out of bound exception is raised.
    exception.expect(IndexOutOfBoundsException.class);
    unitVector(0,0);
    unitVector(4,4);
    unitVector(10,15);
  
  }
  
  @Test
  public void testcopy() {
    for(int i = 0; i < V1_TESTVEC.length; i++)
      assertArrayEquals(V1_TESTVEC[i], copy(V1_TESTVEC[i]),0.);
    
  }
  
  @Test //Matrix implemeted without final? Problem Transposed² not identity
  public void testtranspose() {
    
    for(int i = 0; i < V1_TESTVEC.length; i++){
      for(int j = 0; j< V1_TESTVEC[i].length; j++){
        
        double [][] Testobject = transpose(V1_TESTVEC[i]);
        assertEquals(V1_TESTVEC[i][j], Testobject[j][0],EPSILON);
        assertEquals(1, Testobject[j].length );
      }
    }
      
  }
  
  
  @Test
  public void testplus() {
    
    // test near 0
    v1      = new double[] { 1, 2, 0.123456789123456789123};
    v2      = new double[] {-1,-2,-0.123456789123457000000};
    s1      = 0;
    s2      = 0;
    d       = 0;
    RES_VEC = new double[] {0,0,0 };
    assertArrayEquals(RES_VEC,plus(v1,v2),EPSILON);

//    // test "normal" vectors
//    v1      = new double[] {1.6926, 182262, 0.7625, 2 , 10E20, 4};
//    v2      = new double[] {3, 0.200, 2567E10,-500,3, 2};
//    RES_VEC = new double[] {};
//    assertArrayEquals(RES_VEC,plus(v1,v2),EPSILON);

//    // test numeric loss of percision
//    v1      = new double[] {1,1};
//    v2      = new double[] {1.12345678912345E17,1};
//    RES_VEC = new double[] {1.12345678912345001E17,2};
//    assertArrayEquals(RES_VEC,plus(v1,v2),EPSILON);
    
    
    // test ERR_VEC_DIMENSIONS
    exception.expect(AssertionError.class);
    exception.expectMessage(ERR_VEC_DIMENSIONS);
    
    v1      = new double[] {1,1};
    v2      = new double[] {1};
    RES_VEC = new double[] {};
    assertArrayEquals(RES_VEC,plus(v1,v2),EPSILON);
    
  }
  
  /**
   * testing the TimesplusANDplusTimes function of VMath class
   */
  @Test
  public void testTimesplusANDplusTimes() {
    
    v1      = new double[] { 1,2,3 };
    s1      = 50;
    v2      = new double[] { 1,1,1 };
    RES_VEC = new double[] { 51,101,151 };
    assertArrayEquals(RES_VEC, timesPlus(v1, s1, v2), EPSILON);
    
    s2      = s1;
    RES_VEC = new double[] { 51,52,53 };
    assertArrayEquals(RES_VEC, plusTimes(v1, v2, s2), EPSILON);
    
    
    // Test if assertionError ERR_VEC_DIMENSIONS is raised
    exception.expect(AssertionError.class);
    exception.expectMessage(ERR_VEC_DIMENSIONS);
    
    v1 = new double[] { 1 };
    v2 = new double[] { 1,1 };
    assertArrayEquals(RES_VEC, timesPlus(v1, s1, v2), EPSILON);
    assertArrayEquals(RES_VEC, plusTimes(v1, v2, s2), EPSILON);
   
    
  }
  
}
