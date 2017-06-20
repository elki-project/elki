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
//import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
//import org.junit.rules.RuleChain;
import org.junit.Test;
import static org.junit.Assert.*;



/**
 * Test the V(ector)Math class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */

public class VMathTest {

  /**
   * A small number to handle numbers near 0 as 0.
   */
  private static final double EPSILON = 1E-15;
  
  /**
   * Error message (in assertions!) when vector dimensionalities do not agree.
   */
  private static final String ERR_VEC_DIMENSIONS = "Vector dimensions do not agree.";
  
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  
  // Tests on general (matrix and vector) operations.
  
  @Test //index is given Starting 0. 
  public void testVector_unitVector() {
    
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
    
    // TOIMPL: unitMatrix(dim) QUEST: here?
    
    // Assertion that index out of bound exception is raised.
    exception.expect(IndexOutOfBoundsException.class);
    unitVector(0,0);
    unitVector(4,4);
    unitVector(10,15);
  
  }
  
  /**
   * Test the copy functions of VMath class.
   * 
   * Tested Methods:
   * copy(vector), TOIMPL: copy(Matrix), columPackedCopy(Matrix), rowPackedcCopy(Matrix)
   */
  @Test
  public void testcopy() {
    
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
    
    //TOIMPL: testing copy(Matrix) method
    
    //TOIMPL: testing columPackedCopy(Matrix) method
    
    //TOIMPL: testing rowPackedcCopy(Matrix) method

  }
  
  @Deprecated
  @Test
  public void testplus() {
    // TODO: Merge into testVectorAdd
    double[] v1,v2, RES_VEC;
    

    // test "normal" vectors
    v1      = new double[] {1.6926, 182262    , 0.7625             ,   2, 10E16, 4};
    v2      = new double[] {3     , 0.201     , 2567E10            ,-500,   3, 2};
    RES_VEC = new double[] {4.6926, 182262.201, 25670000000000.7625,-498, 10E16, 6.};
    assertArrayEquals(RES_VEC,plus(v1,v2),EPSILON);

    // test numeric loss of percision
    v1      = new double[] {1                       , 1   , 1};
    v2      = new double[] {1.123456789123456789E-17, 1E17, 1};
    RES_VEC = new double[] {1                       , 1E17, 2};
    assertArrayEquals(RES_VEC,plus(v1,v2),0);
    
    
    // test ERR_VEC_DIMENSIONS
    exception.expect(AssertionError.class);
    exception.expectMessage(ERR_VEC_DIMENSIONS);
    
    v1      = new double[] {1,1};
    v2      = new double[] {1};
    RES_VEC = new double[] {};
    assertArrayEquals(RES_VEC,plus(v1,v2),0);
    
  }
  
  /**
   * Testing the clear() method of VMath class.
   */
  @Test
  public void testclear() {
    // TOIMPL: clear(v1);
    
    // TOIMPL: clear(m);
  }
  
  /**
   * Testing the hashcode() method of VMath class.
   */
  @Test
  public void testhashcode() {
    // TOIMPL: hashCode()
    // TOIMPL: hashCode(v1);
    // TOIMPL: hashCode(m1);
  }
  
  
  // Tests on Vector operations.
  
  /**
   * Testing VMath plus-operations for addition on vectors.
   * 
   * The following VMath methods are tested:
   *  
   * {@link Vmath#plus},{@link Vmath#plusEquals}; {@link Vmath#timesPlus}, {@link Vmath#timesPlusEquals}; 
   * {@link Vmath#plusTimes}, {@link Vmath#plusTimesEquals}, {@link Vmath#timesPlus}, {@link Vmath#timesPlusEquals};
   * {@link Vmath#timesPlustimes}, {@link Vmath#timesPlustimesEquals}
   * QUEST: link in Docstrings or not?
   */
  @Test
  public void testVectorPLUS() {
    
    double s1,s2,d; double[] v1, v2, res_plus, res_plus_scal, res_plusTimes, res_timesPlus, res_timesPlustimes;
    final double delta = 1E-10;
    
    /**
     * TestData I consists of:
     * Four dimensional test vectors, Near 0 numerical loss of precision in ResultData res* and
     * mixed positive and negative numbers.
     * 
     * Notes:
     * Calculated ResultData res* by hand.
     */
    v1                 = new double[] {-14,  1, 2, 0.100000000000006100004};
    v2                 = new double[] {  7,  6, 2,-0.1000000000000069     };
    
                
    res_plus           = new double[] { -7,  7, 4,-0.000000000000000700877}; //  v1 +  v2
    res_plus_scal      = new double[] { -1, 14,15,13.100000000000006100004}; //  v1 +  13
    res_timesPlus      = new double[] {-77, 12,14, 0.500000000000029700024}; // 6v1 +  v2
    res_plusTimes      = new double[] { 28, 37,14,-0.500000000000035299996}; //  v1 + 6v2
    res_timesPlustimes = new double[] {-49,-16,-2, 0.500000000000032900008}; // 2v1 +(-3)v2
    
    // plus  and plusEquals (Vector + Vector) 
    assertArrayEquals(res_plus, plus(v1,v2), EPSILON);
    assertArrayEquals(res_plus, plusEquals(copy(v1), v2), EPSILON);
    
    //plus() and plusEquals (Vector + Skalar)
    d = 13;
    assertArrayEquals(res_plus_scal, plus(v1,d), EPSILON);
    assertArrayEquals(res_plus_scal, plusEquals(copy(v1), d), EPSILON);
    
    // timesPlus() and timesPlusEquals() 
    s1      = 6;
    assertArrayEquals(res_timesPlus, timesPlus(v1, s1, v2), EPSILON);
    assertArrayEquals(res_timesPlus, timesPlusEquals(copy(v1), s1, v2), EPSILON);
    
    // plusTimes() and plusTimesEquals()
    s2      = 6;
    assertArrayEquals(res_plusTimes, plusTimes(v1, v2, s2), EPSILON);
    assertArrayEquals(res_plusTimes, plusTimesEquals(copy(v1), v2, s2), EPSILON);
    
    // timesPlustimes() and timesPlustimesEquals()
    s1      =  2;    s2      = -3;
    assertArrayEquals(res_timesPlustimes, timesPlusTimes(v1, s1, v2, s2), EPSILON);
    assertArrayEquals(res_timesPlustimes, timesPlusTimesEquals(copy(v1), s1, v2, s2), 0);
    
    
    
    
    /**
     * TestData II:
     * Three dimensional test vectors of type double with 5 decimal places as
     * mantissa. Numbers are strictly positive
     * 
     * Notes:
     * Consider v1 + v2. Octave was used to aid calculation of ResultData res*.
     */  
    v1                 = new double[] { 0.17825, 32.546, 2958.3 };
    v2                 = new double[] { 0.82175, 67.454, 7041.7 };
    
                
    res_plus           = new double[] { 1       , 100      , 10000      }; //    v1 +   v2
    res_plus_scal      = new double[] { 1.1     ,  33.46775,  2959.22175}; //    v1 +  0.92175
    res_timesPlus      = new double[] { 1.089125, 116.273  , 11479.15   }; // 1.5v1 +   v2
    res_plusTimes      = new double[] { 1.410875, 133.727  , 13520.85   }; //    v1 +1.5v2
    res_timesPlustimes = new double[] { 0.67825 , 82.546   ,  7958.3    }; // 1.5v1 +0.5v2
    
    // plus  and plusEquals (Vector + Vector) 
    assertArrayEquals(res_plus, plus(v1,v2), delta);
    assertArrayEquals(res_plus, plusEquals(copy(v1), v2), delta);
    
    //plus() and plusEquals (Vector + Skalar)
    d = 0.92175;
    assertArrayEquals(res_plus_scal, plus(v1,d), delta);
    assertArrayEquals(res_plus_scal, plusEquals(copy(v1), d), delta);
    
    // timesPlus() and timesPlusEquals() 
    s1      = 1.5;
    assertArrayEquals(res_timesPlus, timesPlus(v1, s1, v2), delta);
    assertArrayEquals(res_timesPlus, timesPlusEquals(copy(v1), s1, v2), delta);
    
    // plusTimes() and plusTimesEquals()
    s2      = 1.5;
    assertArrayEquals(res_plusTimes, plusTimes(v1, v2, s2), delta);
    assertArrayEquals(res_plusTimes, plusTimesEquals(copy(v1), v2, s2), delta);
    
    // timesPlustimes() and timesPlustimesEquals()
    s1      =  1.5; s2      =  0.5;
    assertArrayEquals(res_timesPlustimes, timesPlusTimes(v1, s1, v2, s2), delta);
    assertArrayEquals(res_timesPlustimes, timesPlusTimesEquals(copy(v1), s1, v2, s2), delta);
    
  }
  
  /**
   * Testing VMath minus-operations for subtraction on vectors.
   * 
   * (vector - vector, multiplied with scalars & vector - scalar)
   * 
   * The following VMath methods are tested:
   * minus, minusEquals; timesMinus, timesMinusEquals; 
   * minusTimes, minusTimesEquals, timesMinus, timesMinusEquals;
   * timesMinustimes, timesMinustimesEquals
   * 
   */
  @Test
  public void testVectorMINUS() {
    double s1,s2,d; double[] v1, v2, res_plus, res_plus_scal, res_plusTimes, res_timesPlus, res_timesPlustimes;
    final double delta = 1E-10;
    
    /**
     * TODO TestData I:
     * 
     * Octave was used to aid calculation of ResultData res*.
     */
    
    
    
    /**
     * TODO TestData II:
     * 
     * Octave was used to calculate ResultData res*;
     */
  }

  /**
   * Testing VMath Vector Products. 
   * 
   * scalarProduct, 3D Cross Product, Vector*scalar, ...
   * 
   * 
   */
  @Test
  public void testVectorPORDUCTS() {
    
    // TOIMPL: test for times()
    
    // TOIMPL: test for cross3D()
    
    // TOIMPL: test for scalarProduct()
    
    // QUEST: implement tests for transposedtimes(), timestransposed() here.
    // QUEST: isn't transposedtimes equal to scalarProduct --> kick one!

  }

  /**
   * Testing the normalize(Vector) methods of the VMath class.
   * 
   * normalize(), normalizeEquals()
   */
  @Test
  public void testVectorNORMALIZE() {
    // TOIMPL: normalize()
    // TOIMPL: normalizeEquals()
  }
  
  /**
   * Testing the angle(Vector, Vector) methods of the VMath class.
   * 
   * 
   */
  @Test
  public void testVectorANGLE() {
    
    // TOIMPL: angle(v1, v2);
    
    // TOIMPL: angle(v1, v2, o);
    
  }
  
  /**
   * Testing the sum methods of VMath class.
   */
  @Test
  public void testVectorsum() {
    // TOIMPL: sum(v1)
    // TOIMPL: squareSum(v1)
    // TOIMPL: euclideanLength(v1) QUEST: Here or extra method?
  }
    
  /**
   * Testing the transposed method of VMath class.
   */
  @Test // QUEST:Matrix implemeted without final? Problem Transposed² not identity
  public void testVector_transposed() {
    //TODO: get rid of V1_TESTVEC deprecated
    double[] v1; double[][] res;
    
    v1  = new double[] {0.12345, 1E25 , 1};
    res = transpose(v1);
    
    for(int i = 0; i< v1.length; i++){
      assertEquals(v1[i], res[i][0],0);
      assertEquals(1, res[i].length );
    }
    
    v1 = new double[] {4,5,6,0,2,1};
    res = transpose(v1);
    
    for(int i = 0; i< v1.length; i++){
      assertEquals(v1[i], res[i][0],0);
      assertEquals(1, res[i].length );
    }
      
  }

  /**
   * Testing the rotate90 method of VMath class.
   */
  @Test
  public void testVector_rotate90() { // QUEST: notation of methods?
    // TOIMPL: rotate90Equals(v1)
  }

  //QUEST: REF method instead of VectorREF Missmach?
  /**
   * Testing that *Equals vector-operations of the {@link VMath} class work in place and the other operations don't.
   *  
   *  
   * This class tests the VMath methods as in {@link #testVectorPLUS } {@link #testVectorMINUS} and ...
   */
  @Test
  public void testVectorREF() {
    final double[] v2 = {0};
    double[] v1 = v2;
    final double s1 = 0;
    final double s2 = 0;
    final double d  = 0;
    
    // VectorADD
    assertNotSame(v1, plus(v1, d));
    assertSame(v1, plusEquals(v1, d));
    
    assertNotSame(v1, plus(v1, v2));
    assertSame(v1, plusEquals(v1, v2));
    
    assertNotSame(v1, timesPlus(v1, s1, v2));
    assertSame(v1, timesPlusEquals(v1, s1, v2));
    
    assertNotSame(v1, plusTimes(v1, v2, s2));
    assertSame(v1, plusTimesEquals(v1, v2, s2));
    
    assertNotSame(v1, timesPlus(v1, s1, v2));
    assertSame(v1, timesPlusEquals(v1, s1, v2));
    
    assertNotSame(v1, timesPlusTimes(v1, s1, v2, s2));
    assertSame(v1, timesPlusTimesEquals(v1, s1, v2, s2));
    
    // VectorMINUS
    assertNotSame(v1, minus(v1, d));
    assertSame(v1, minusEquals(v1, d));
    
    assertNotSame(v1, minus(v1, v2));
    assertSame(v1, minusEquals(v1, v2));
    
    assertNotSame(v1, timesMinus(v1, s1, v2));
    assertSame(v1, timesMinusEquals(v1, s1, v2));
    
    assertNotSame(v1, minusTimes(v1, v2, s2));
    assertSame(v1, minusTimesEquals(v1, v2, s2));
    
    assertNotSame(v1, timesMinus(v1, s1, v2));
    assertSame(v1, timesMinusEquals(v1, s1, v2));
    
    assertNotSame(v1, timesMinusTimes(v1, s1, v2, s2));
    assertSame(v1, timesMinusTimesEquals(v1, s1, v2, s2));

  }
  
 
  // QUEST: DimensionMissmach method instead of VectorDim missmach?
  /**
   * testing that Error is Vector dimension missmach is raised if input data of Vector Operation has different dimensions.
   * 
   * FIXME: Class not working properly. Break after the first Assertation error.
   * Solution: Probably  with ErrorCollector Rule, but did not work in combination with expect expectation.
   * Try with Test(expectation=AssertationError.class) but not good because error code may not be ERR_VEC_Dimesions
   */
  @Test
  public void testVectorDimensionMissmach() {
    // Test if assertionError ERR_VEC_DIMENSIONS is raised
    exception.expect(AssertionError.class);
    exception.expectMessage(ERR_VEC_DIMENSIONS);
    
    final double[] v1      = {1,1};
    final double[] v2      = {1};
    double s1,s2; s1 = s2 = 1;

    
    // 
    plus(v1, v2);
    plusEquals(v1, v2);
    
    timesPlus(v1, s1, v2);
    timesPlusEquals(v1, s1, v2);
    
    plusTimes(v1, v2, s2);
    plusTimesEquals(v1, v2, s2);
    
    timesPlusTimes(v1, s1, v2, s2);
    timesPlusTimesEquals(v1, s1, v2, s2);
    
    
 
  
  }
  
  
  //Tests on Matrix operations.
  
  /**
   * Testing the Matrix plus operations of VMath class.
   */
  @Test
  public void testMatrixPLUS() {
    // TOIMPL: plus(m1, m2); plusEquals(m1, m2); plusTimes(m1, m2, s2) ... as in VectorPLUS 
  }
  
  /**
   * Testing the Matrix minus operations of VMath class.
   */
  @Test
  public void testMatrixMINUS() {
    // TOIMPL: minus(m1, m2) ... as in VectorMinus 
  }
  
  /**
   * Testing the _transposed method of VMath class.
   */
  @Test
  public void testMatrix_transposed() {
    // TOIMPL: transpose(m1)
  }
  
  /**
   * Testing the Matrix MULTIPLICATION methods of VMath class.
   */
  @Test
  public void testMatrixMULTIPLICATION() {
    // TOIMPL: Matrix Scalar multiplication & transposed.
//    times(m1, s1); timesEquals(m1, s1);
    
    // TOIMPL: Matrix, Matrix multiplication & transposed.
//    times(m1, m2); 
//    timesTranspose(m1, m2);
//    transposeTimes(m1, m2);
//    transposeTimesTranspose(m1, m2);
    
    // TOIMPL: Vector and Matrix multiplication & transposed.
//    times(m1, v2); 
//    transposeTimes(m1, v2);
//    transposeTimesTimes(a, B, c);
    
    
//QUEST: times(v1, m2) vs transposeTimes(v1, m2) dublicate or what ? --> answer on timesTranspose(v1, m2);
//FIXME: transposed and times notation. 
 
  }
  
  /**
   * Testing the initialization and setting methods of VMath class on Matrix's. 
   */
  @Test
  public void testMatrixSET() {
    // Set a Matrix or a part of a Matrix.
    // TOIMPL: setCol(m1, c, column); setRow(m1, r, row);
    // TOIMPL: setMatrix(m1, r, c, m2); setMatrix(m1, r0, r1, c, m2); setMatrix(m1, r, c0, c1, m2); setMatrix(m1, r0, r1, c0, c1, m2);
  
    // TOIMPL: unitMatrix(dim); identity(m, n) QUEST: implement or create a unit&identity testclass with Vector
    // TOIMPL: diagonal(v1);
  }
  
  /**
   * Testing the GET method of VMath class.
   */
  @Test
  public void testMatrixGET() {
    // Get a Submatix.
    // TOIMPL: getMatrix(m1, r, c); getMatrix(m1, r0, r1, c); getMatrix(m1, r, c0, c1); getMatrix(m1, r0, r1, c0, c1);
    
    // Get colums of properties of am Matrix.
    // TOIMPL: getCol(m1, col); getRow(m1, r)
    // TOIMPL: getRowDimensionality(m1);    getColumnDimensionality(m1);
    // TOIMPL: getDiagonal(m1);
    
  }
  
  /**
   * Testing the _almosteq method of VMath class.
   */
  @Test
  public void testMatrix_almosteq() {
    // TOIMPL: almostEquals(m1, m2); almostEquals(m1, m2, maxdelta);
  }

  /**
   * Testing the _solve method of VMath class.
   */
  @Test
  public void testMatrix_solve() {
    // TOIMPL: solve(A, B);
  }
  
  /**
   * Testing the _appendColums method of VMath class.
   */
  @Test
  public void testMatrix_appendColums() {
    // TOIMPL: appendColumns(m1, m2);
  }
  
  /**
   * Testing the Matrix_project method of VMath class.
   */
  @Test
  public void testMatrix_project() {
    // TOIMPL: project(v1, m2)
  }

  /**
   * Testing the inverse method of VMath class.
   */
  @Test
  public void testinverse() {
    // TOIMPL: inverse(elements)
  }

  
}
