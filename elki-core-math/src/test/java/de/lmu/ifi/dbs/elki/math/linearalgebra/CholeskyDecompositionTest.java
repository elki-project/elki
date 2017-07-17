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
import de.lmu.ifi.dbs.elki.math.linearalgebra.CholeskyDecomposition;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author Merlin Dietrich
 *
 */
public final class  CholeskyDecompositionTest {

  private static double[][] TESTMATRIX_L1 = {
      { 1,    0,   0,   0,   0,   0,   0,   0,  0, 0,}, // 
      { 7,   65,   0,   0,   0,   0,   0,   0,  0, 0,}, // 
      { 62, -78,  90,   0,   0,   0,   0,   0,  0, 0,}, // 
      { 77,  88,  -6,  35,   0,   0,   0,   0,  0, 0,}, // 
      { 27, -41,  -8,  30,  34,   0,   0,   0,  0, 0,}, // 
      { 59, -48,  44,   4,  20,  83,   0,   0,  0, 0,}, // 
      {-48, -63, -38,  35, -30,  81,  87,   0,  0, 0,}, // 
      {-74,  69,  68, -65,  16, -92, -29,  87,  0, 0,}, // 
      {-88, -39,  23,  81,  52, -95,  16, -90, 14, 0,}, // 
      { 20, -80, -57,  14, -95, -47, -43, -26, 29, 5,}, // 
  };
  
  private static double[][] TESTMATRIX_L2 = {
    { 1969.5249057431356,     0               ,     0              ,     0              ,     0              ,     0              ,     0             ,    0             ,     0              ,     0             ,}, //
    {-3778.931190321073 ,  2130.195233591261  ,     0              ,     0              ,     0              ,     0              ,     0             ,    0             ,     0              ,     0             ,}, //
    { 7897.304228880785 ,  4761.858130790322  ,  6641.458678884961 ,     0              ,     0              ,     0              ,     0             ,    0             ,     0              ,     0             ,}, //
    {-8224.187898172215 ,   650.9449507055269 , -5838.595185335999 ,  5370.828810576402 ,     0              ,     0              ,     0             ,    0             ,     0              ,     0             ,}, //
    {-7478.413734477626 , -6581.054183150885  , -5869.786738350511 , -2461.671702479478 ,  1729.6165087199442,     0              ,     0             ,    0             ,     0              ,     0             ,}, //
    { 5067.819613705262 ,   -42.12205881216869, -2491.706436955512 , -6027.931613844906 , -9690.476641715957 ,  5532.31979037159  ,     0             ,    0             ,     0              ,     0             ,}, //
    { 4230.302159576424 ,  7047.433974545002  , -3955.4819232900054, -7700.679118622675 ,   623.4986745110509,  6357.883612580732 ,  9448.074435856885,    0             ,     0              ,     0             ,}, //
    {-9587.321218537803 ,  2090.916311928586  , -1463.761225539749 , -2616.0553607241345, -8948.257909568407 ,  5722.836886508077 , -9373.774396775283, 8588.838590029674,     0              ,     0             ,}, //
    { 4547.638194970525 ,  9171.289509833565  ,  1701.224402453252 ,  9691.325885593764 , -1347.9994226315175, -4794.137302478112 ,  9140.72950573163 , 8432.893607777965,  8708.357809689789 ,     0             ,}, //
    { 7187.132905478913 , -2018.6217701736368 ,  2434.896101267208 ,  5357.489474821838 ,  2065.6526281452225, -4048.3345966106144, -3528.384894203782, 6580.45600445064 ,  5616.0250457222355,  5532.960015167152,}, //
};
  /**
   * TODO Comment and test solve method
   */
  @Test
  public void testConstructor() {
    CholeskyDecomposition CholL1 = new CholeskyDecomposition(timesTranspose(TESTMATRIX_L1, TESTMATRIX_L1));
    CholeskyDecomposition CholL2 = new CholeskyDecomposition(timesTranspose(TESTMATRIX_L2, TESTMATRIX_L2));
    
    assertThat(CholL1.getL(), is(equalTo(TESTMATRIX_L1)));
    assertTrue(almostEquals(CholL2.getL(), TESTMATRIX_L2));
  }
  
  /**
   * Testing the isSPD (is symmetric positive definite) method of CholeskyDecomposition class. 
   */
  @Test
  public void testIsSPD() {

    CholeskyDecomposition CholL1 = new CholeskyDecomposition(timesTranspose(TESTMATRIX_L1, TESTMATRIX_L1));
    CholeskyDecomposition CholL2 = new CholeskyDecomposition(timesTranspose(TESTMATRIX_L2, TESTMATRIX_L2));

    assertTrue(CholL1.isSPD());
    assertTrue(CholL2.isSPD());
    
    // symmetric but not positive definite Matrix
    final double[][] A3 = {
      {-1,   3, 2}, //
      { 3, -12, 2}, //
      { 2,   2, 5}, //
    };
    
    // non symmetric positive definite Matrix
    final double[][] A4 = {
        { 1,   3}, //
        { 2,  12}, //
    };
    
    CholeskyDecomposition CholL3 = new CholeskyDecomposition(A3);
    CholeskyDecomposition CholL4 = new CholeskyDecomposition(A4);
    
    assertFalse(CholL3.isSPD());
    assertFalse(CholL4.isSPD());
  }
  
}
