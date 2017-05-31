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
  private static final double EPSILON = 1E-16;
  
  /**
   * A collection of v1 testvectors for later use.
   */
  private static final double[][] V1_TESTVEC = { 
      {},
      {1,2,3},
      {4,5,6,0},
  };
 
  
  
  @Test //sqrt implemented was missing at first. Case 0 not working jet
  public void testrandomNormalizedVector() {
    
    for(int i = 1; i < 2; i++){
      final double[] v = randomNormalizedVector(i);
      double vnorm = 0;
      for(int j=0; j < i;j++) {
        final double x = v[j];
        vnorm += x*x;
      };
      assertEquals(1,vnorm,EPSILON); 
    };
    
  }
  
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test //index is given Starting 0. 
  public void testunitVector() {
    
    assertArrayEquals(new double[] {1,0,0}, unitVector(3,0),0. );
    assertArrayEquals(new double[] {0,1,0}, unitVector(3,1),0. );
    assertArrayEquals(new double[] {0,0,1}, unitVector(3,2),0. );

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
        
    double[] v = {0,0,0 };
    double[] v1 ={1,2, 0.123456789123456789123};
    double[] v2 = {-1,-2,-0.12345678912345678000};
    assertArrayEquals(v,plus(v1,v2),EPSILON);
  
  }
  
  /**
   *
   *
   */
//  @Test
//  public void EG() {
//  
//    assertArrayEquals(v, foo(), EPSILON);
//
//    assertEquals(d, foo(), EPSILON);
//    
//  }
//  
  
  
}
