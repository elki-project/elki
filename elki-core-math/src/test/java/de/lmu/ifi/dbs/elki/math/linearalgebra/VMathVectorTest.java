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
import java.math.*;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Rule;
//import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
//import org.junit.rules.RuleChain;
import org.junit.Test;



/**
 * Test the V(ector)Math class.
 *
 * @see VMath
 * @author Merlin Dietrich
 * 
 */
public final class VMathVectorTest {

  private static final double EPSILON = VMathOperationsTest.EPSILON;
  
  /**
   * Error message (in assertions!) when vector dimensionalities do not agree.
   * @see VMath#ERR_VEC_DIMENSIONS
   */
  protected static final String ERR_VEC_DIMENSIONS = "Vector dimensions do not agree.";
  
  /**
   * A vector of length 5 for testing
   */
  protected static final double[] TESTVEC = {2,3,5,7,9};
  
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

  
  @Deprecated
  @Test
  public void testplus() {
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
    
//    v1      = new double[] {1,1};
//    v2      = new double[] {1};
//    RES_VEC = new double[] {};
//    assertArrayEquals(RES_VEC,plus(v1,v2),0);
//    
  }

  
  // Tests on Vector operations.
  

  
  /**
   * Testing the times(vector, scalar) method of {@link VMath} class.
   */
  @Test
  public void testTimes() {
    
    final double[] v1       = {1,3,9,7}; 
    final double   s_case1  = 5;
    final double   s_case2  = 1/3;
    final double   s_case3  = 0.100000000000006100004;
    
    final double[] res_case1  = {v1[0]*s_case1,v1[1]*s_case1,v1[2]*s_case1,v1[3]*s_case1};
    final double[] res_case2  = {v1[0]*s_case2,v1[1]*s_case2,v1[2]*s_case2,v1[3]*s_case2};
    final double[] res_case3  = {v1[0]*s_case3,v1[1]*s_case3,v1[2]*s_case3,v1[3]*s_case3};
    
    // test the three cases
    assertArrayEquals(res_case1, times(v1,s_case1), 0.);
    assertArrayEquals(res_case2, times(v1,s_case2), EPSILON);
    assertArrayEquals(res_case3, times(v1,s_case3), EPSILON);
    
  }
  
  /**
   * Testing {@link VMath} plus-operations for addition on vectors.
   * <p>
   * The following VMath methods are tested:<br>
   * {@link Vmath#plus},{@link Vmath#plusEquals}; {@link Vmath#timesPlus}, {@link Vmath#timesPlusEquals}; 
   * {@link Vmath#plusTimes}, {@link Vmath#plusTimesEquals}, {@link Vmath#timesPlus}, {@link Vmath#timesPlusEquals};
   * {@link Vmath#timesPlustimes}, {@link Vmath#timesPlustimesEquals}
   */
  @Test
  public void testPlus() {
    // TODO: Degenerate cases: and comment
    
    // TODO: redefine structure with final double res vector and manual implementation of computations in. assert equality of method . .Equals
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
    
    double[] out, in_eq; //FIXME: Question _ not in variable in Java?
    
    // plus  and plusEquals (Vector + Vector) 
    assertArrayEquals(res_plus, out = plus(v1,v2), EPSILON);
    assertArrayEquals(res_plus, plusEquals(in_eq = copy(v1), v2), EPSILON);
    assertArrayEquals(out, in_eq, 0); // assert methods doing the same //TODO: beispiel fuer Vortrag
    
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
   * Testing {@link VMath} minus-operations for subtraction on vectors.
   * <p> 
   * The following VMath methods are tested:<br>
   * minus, minusEquals, timesMinus, timesMinusEquals; 
   * minusTimes, minusTimesEquals, timesMinus, timesMinusEquals;
   * timesMinustimes, timesMinustimesEquals
   */
  @Test
  public void testMinus() {
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
   * Testing {@link VMath} Vector scalarProduct and transposeTimes methods
   */
  @Test
  public void testScalarProduct() {
    
    // testing scalarProduct(vector, vector)
    final double[] v1 = {1.21, 3 , 7 ,-2};
    final double[] v2 = {3.5 , -1, 4 ,-7};
    final double res = v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2]+v1[3]*v2[3];
    
    assertEquals(transposeTimes(v1, v2), scalarProduct(v1, v2), EPSILON);
    assertEquals(res, scalarProduct(v1, v2), EPSILON);
    assertEquals(squareSum(v1), scalarProduct(v1, v1), EPSILON);
    
    
    // FIXME: Question implement tests for transposedtimes(), timestransposed() here.
  }
  
  /**
   * Testing the timesTranspose(vector, vector) method of {@link VMath} class. 
   */
  @Test
  public void testTimesTranspose() {
 
    final double[] v1 = {1.21, 3 , 7 ,-2};
    final double[] v2 = {3.5 , -1, 4 ,-7};
    
    final double[][] res = {
        {v1[0]*v2[0], v1[0]*v2[1], v1[0]*v2[2], v1[0]*v2[3]},
        {v1[1]*v2[0], v1[1]*v2[1], v1[1]*v2[2], v1[1]*v2[3]},
        {v1[2]*v2[0], v1[2]*v2[1], v1[2]*v2[2], v1[2]*v2[3]},
        {v1[3]*v2[0], v1[3]*v2[1], v1[3]*v2[2], v1[3]*v2[3]} };
    
    assertThat(timesTranspose(v1, v2), is(equalTo(res)));
    
    // testing timesTranspose(vector, vector) via times(matrix, matrix)
    // because times(vector) returns a matrix. This is at the same time a test for transpose if timesTranspose is correct.
    final double[][] m1 = transpose(transpose(TESTVEC));
    final double[][] m2 = transpose(TESTVEC);
    
    assertThat(timesTranspose(TESTVEC, TESTVEC), is(equalTo(times(m1, m2))));
  }

  /**
   * Testing the normalizeVector) and normalizeEquals(Vector) methods of the {@link VMath} class.
   * 
   * 
   */
  @Test
  public void testNormalize() {
    
    final double[] v1 = copy(TESTVEC);
    final double[] v1_copy = copy(v1);
    final double[] v1_normal = normalize(v1);
    
    // Test that both methods return a vector with length 1
    // that more methods ensure the testresult we use squareSum instead of euclideanLength here
    assertEquals(1, squareSum(v1_normal), EPSILON);
    assertEquals(1, squareSum(normalizeEquals(v1)), EPSILON);
    
    // Check that both methods return the same Vector
    assertArrayEquals(v1_normal, v1, EPSILON);
    
    // Check that the normalize Vector times the euclidean length of the original Vector equals the original vector
    assertArrayEquals(v1_copy, times(v1_normal, euclideanLength(v1_copy)) , EPSILON);

  }

  /**
   * Testing the angle(Vector, Vector) methods of the {@link VMath} class.
   * 
   * 
   */
  @Test
  public void testAngle() {
    // TODO: Implement: angle(v1, v2);
    final double[] v1 = {1,0};
    final double[] v2 = {1,1};
    
//    assertEquals(0.5, angle(v1, v2), EPSILON);
    assertEquals(Math.sin(Math.PI/4), angle(v2, v1), EPSILON);
    // comment on this strange format
    
    // This is a mathimatically strange method it esecially calculate the sclarproduct of two vectors and gives back the normaliy 
    
    // FIXME: Question new class?
    // TODO: Implement: angle(v1, v2, o);
    
  }
  
  /**
   * Testing the sum methods and the euclidienLength method of {@link VMath} class.
   */
  @Test
  public void testSum() {
    // TODO: more defindes testing Implement: 
    // testing sum(vector)
    final double[] v = {1.21, -3, 2, 3.2, -5, 1};
    final double res_vsum = v[0]+v[1]+v[2]+v[3]+v[4]+v[5];
    assertEquals(res_vsum, sum(v), EPSILON);
    
    assertEquals(1 , sum(unitVector(3, 1)), 0.);
    
    final double[] v1 = { 0.1234512345123456 , - 0.123451234512345};
    assertEquals( 0, sum(v1), EPSILON);
    
    // testing squareSum(vector)
    final double res_vsumsqare = v[0]*v[0]+v[1]*v[1]+v[2]*v[2]+v[3]*v[3]+v[4]*v[4]+v[5]*v[5];
    assertEquals(res_vsumsqare, squareSum(v), 0.);
    assertEquals(1 , squareSum(unitVector(20, 10)), 0.);
    
    // testing euclideanLength(vector) FIXME: Question Here or extra method?
    final double res_veuclid = Math.sqrt(res_vsumsqare);
    assertEquals(res_veuclid, euclideanLength(v), EPSILON);
    assertEquals(1 , euclideanLength(unitVector(3, 1)), 0.);
  }
    
  /**
   * Testing the transpose(vector) method of {@link VMath} class.
   */
  @Test 
  public void testTransposed() {
    
    final double[] v1 = TESTVEC;
    final double[][] v1_t = {v1};
    
    assertThat(transpose(v1), is(equalTo(v1_t)));
    
    final double[] v2 = {4,5,6,0,2,1};
    final double[][] v2_t = {v2};
    
    assertThat(transpose(v2), is(equalTo(v2_t)));
  }

  /**
   * Testing the rotate90Equals(vector) method of {@link VMath} class.
   */
  @Test
  public void testVectorRotate90Equals() { 
    // simple testcase
    final double[] v1 = {1, 0};
    final double[] res = {0,-1};
    assertArrayEquals(res, rotate90Equals(v1), 0);
   
    // more complex testcase TODO: use angle method if it is not deprecated
    final double[] v2 = {1.21, -2.4};
    final double[] v2_copy = copy(v2);
    assertEquals(0.0, scalarProduct(v2_copy, rotate90Equals(v2)), EPSILON);
    final double[] v2_copy1rotate = copy(v2);
    assertArrayEquals(times( v2_copy, -1), rotate90Equals(v2), EPSILON);
    assertArrayEquals(times( v2_copy1rotate, -1), rotate90Equals(v2), EPSILON);
    assertArrayEquals(v2_copy, rotate90Equals(v2), EPSILON);
    
  }

  /**
   * Testing that *Equals vector-operations of the {@link VMath} class work in place and testing that other vector-operations create a new instance.
   * 
   * Tests of vector-methods where the class of the instance returned differs form class of input method are reasonably omitted,
   * when testing reference e.g. transposed.
   * <p>
   * For a complete list of tested methods: <br>
   * {@link #testMinus}, {@link #testNormalize}, {@link #testPlus}, {@link #testTimes}, {@link #testRotate90Equals} 
   */
  @Test
  public void testVectorReference() {
    final double[] v1 = {0}, v2 = {0};
    final double s1 = 0, s2  = 0;
    
    // testing methods as in testAngele is not needed
    
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
    final double[] v3 = {2};
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
 
    // testing methods as in testScalarProduct is not needed
    // testing methods as in testSum is not needed
    
    // testing methods as in testTimes
    assertNotSame(v1, times(v1, s1));
    assertSame(v1, timesEquals(v1, s1));
    
    // testing methods as in testTimesTranspose is not needed
    // testing methods as in testTranspose is not needed
    // testing methods as in testUnitVector is not needed
       
    // testing methods as in testRotate90Equals
    final double[] v4 = {1,0};
    assertSame(v4 , rotate90Equals(v4));
  }
 
  /**
   * testing that Error is Vector dimension missmach is raised if input data of Vector Operation has different dimensions.
   */
  @Test
  public void testVectorDimensionMissmach() {

    final double[] v_len1 = {1};
    final double[] v_len2 = {1,1};
    final double[] v_len3 = {1,1,1};
    double s1,s2; s1 = s2 = 1;
    
    // test angle not needed
    // test Normalize is not needed
    
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
    
    // test for methods as in testSum is not needed
    // test timesTranspose is no needed
    
    // unitVector only use index out of Bound exception in assertDimensionMismatch
    assertDimensionMismatch("", () -> unitVector(0,0));
    assertDimensionMismatch("", () -> unitVector(4,4));
    assertDimensionMismatch("", () -> unitVector(10,15));
    
    // test rotate90
    assertDimensionMismatch("rotate90Equals is only valid for 2d vectors.", () -> rotate90Equals(v_len1));
    assertDimensionMismatch("rotate90Equals is only valid for 2d vectors.", () -> rotate90Equals(v_len3)); 
  }

  
}
