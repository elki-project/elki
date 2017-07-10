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
        
    /**
     * TestData I consists of:
     * Four dimensional test vectors, Near 0 numerical loss of precision in ResultData res* and
     * mixed positive and negative numbers.
     */
    final double[] v1_I                 = {-14,  1, 2, 0.100000000000006100004};
    final double[] v2_I                 = {  7,  6, 2,-0.1000000000000069     };
    
    final double s_I = 13, s1_I =  2, s2_I = -3;
                
    final double[] res_plus_I           = {v1_I[0]+v2_I[0], v1_I[1]+v2_I[1], v1_I[2]+v2_I[2], v1_I[3]+v2_I[3]}; //  v1_I +  v2_I
    final double[] res_plus_scal_I      = {v1_I[0]+s_I, v1_I[1]+s_I, v1_I[2]+s_I, v1_I[3]+s_I}; //  v1_I +  13
    final double[] res_timesPlus_I      = {s1_I*v1_I[0]+v2_I[0], s1_I*v1_I[1]+v2_I[1], s1_I*v1_I[2]+v2_I[2], s1_I*v1_I[3]+v2_I[3]}; //  v1_I + 6v2
    final double[] res_plusTimes_I      = {v1_I[0]+s2_I*v2_I[0], v1_I[1]+s2_I*v2_I[1], v1_I[2]+s2_I*v2_I[2], v1_I[3]+s2_I*v2_I[3]}; // 6v1 +  v2_I
    final double[] res_timesPlustimes_I = {s1_I*v1_I[0]+s2_I*v2_I[0], s1_I*v1_I[1]+s2_I*v2_I[1], s1_I*v1_I[2]+s2_I*v2_I[2], s1_I*v1_I[3]+s2_I*v2_I[3]}; // 2v1 +(-3)v2_I
    
    double[] out, in_eq; //FIXME: Question _ not in variable in Java?
    
    // plus  and plusEquals (Vector + Vector) 
    assertArrayEquals(res_plus_I, out = plus(v1_I,v2_I), EPSILON);
    assertArrayEquals(res_plus_I, plusEquals(in_eq = copy(v1_I), v2_I), EPSILON);
    assertArrayEquals(out, in_eq, 0); // assert methods doing the same //TODO: beispiel fuer Vortrag
    
    //plus() and plusEquals (Vector + Skalar)
    assertArrayEquals(res_plus_scal_I, plus(v1_I,s_I), EPSILON);
    assertArrayEquals(res_plus_scal_I, plusEquals(copy(v1_I), s_I), EPSILON);
    
    // timesPlus() and timesPlusEquals() 
    assertArrayEquals(res_timesPlus_I, timesPlus(v1_I, s1_I, v2_I), EPSILON);
    assertArrayEquals(res_timesPlus_I, timesPlusEquals(copy(v1_I), s1_I, v2_I), EPSILON);
    
    // plusTimes() and plusTimesEquals()
    assertArrayEquals(res_plusTimes_I, plusTimes(v1_I, v2_I, s2_I), EPSILON);
    assertArrayEquals(res_plusTimes_I, plusTimesEquals(copy(v1_I), v2_I, s2_I), EPSILON);
    
    // timesPlustimes() and timesPlustimesEquals()
    assertArrayEquals(res_timesPlustimes_I, timesPlusTimes(v1_I, s1_I, v2_I, s2_I), EPSILON);
    assertArrayEquals(res_timesPlustimes_I, timesPlusTimesEquals(copy(v1_I), s1_I, v2_I, s2_I), 0);
    
    
    /**
     * TestData II:
     * Three dimensional test vectors of type double with 5 decimal places as
     * mantissa. Numbers are strictly positive
     * 
     */  
    final double delta = 1E-10;
    final double[] v1_II                 = { 0.17825, 32.546, 2958.3 };
    final double[] v2_II                 = { 0.82175, 67.454, 7041.7 };
    
                
    final double s_II = 0.92175, s1_II =  1.5, s2_II =  0.5;
                
    final double[] res_plus_II           = {v1_II[0]+v2_II[0], v1_II[1]+v2_II[1], v1_II[2]+v2_II[2]}; //  v1_II +  v2_II
    final double[] res_plus_scal_II      = {v1_II[0]+s_II, v1_II[1]+s_II, v1_II[2]+s_II}; //  v1_II +  13
    final double[] res_timesPlus_II      = {s1_II*v1_II[0]+v2_II[0], s1_II*v1_II[1]+v2_II[1], s1_II*v1_II[2]+v2_II[2]}; //  v1_II + 6v2
    final double[] res_plusTimes_II      = {v1_II[0]+s2_II*v2_II[0], v1_II[1]+s2_II*v2_II[1], v1_II[2]+s2_II*v2_II[2]}; // 6v1 +  v2_II
    final double[] res_timesPlustimes_II = {s1_II*v1_II[0]+s2_II*v2_II[0], s1_II*v1_II[1]+s2_II*v2_II[1], s1_II*v1_II[2]+s2_II*v2_II[2]}; // 2v1 +(-3)v2_II

    // plus  and plusEquals (Vector + Vector) 
    assertArrayEquals(res_plus_II, plus(v1_II,v2_II), delta);
    assertArrayEquals(res_plus_II, plusEquals(copy(v1_II), v2_II), delta);
    
    //plus() and plusEquals (Vector + Skalar)
    assertArrayEquals(res_plus_scal_II, plus(v1_II,s_II), delta);
    assertArrayEquals(res_plus_scal_II, plusEquals(copy(v1_II), s_II), delta);
    
    // timesPlus() and timesPlusEquals() 
    assertArrayEquals(res_timesPlus_II, timesPlus(v1_II, s1_II, v2_II), delta);
    assertArrayEquals(res_timesPlus_II, timesPlusEquals(copy(v1_II), s1_II, v2_II), delta);
    
    // plusTimes() and plusTimesEquals()
    assertArrayEquals(res_plusTimes_II, plusTimes(v1_II, v2_II, s2_II), delta);
    assertArrayEquals(res_plusTimes_II, plusTimesEquals(copy(v1_II), v2_II, s2_II), delta);
    
    // timesPlustimes() and timesPlustimesEquals()
    assertArrayEquals(res_timesPlustimes_II, timesPlusTimes(v1_II, s1_II, v2_II, s2_II), delta);
    assertArrayEquals(res_timesPlustimes_II, timesPlusTimesEquals(copy(v1_II), s1_II, v2_II, s2_II), delta);
    
    
    // General testing
    final double[] v5                 = {1,2,3};
    final double[] v6                 = {4,5,6};              
    final double s5 =  2, s6 =  3;
    
    // consistency check to minus method
    assertArrayEquals(minus(v5, times(v6, -1)), plus(v5, v6), 0.);
    // consistency check within the plus methods
    assertArrayEquals(plus(v5, times(v6, s6)), plusTimes(v5, v6, s6), 0.);    
    assertArrayEquals(plus(times(v5, s5), v6), timesPlus(v5, s5, v6), 0.);
    assertArrayEquals(plus(times(v5, s5), times(v6, s6)), timesPlusTimes(v5, s5, v6, s6), 0.);
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
    // Testcase I
    final double[] v1                 = {-14,  1, 2, 0.100000000000006100004};
    final double[] v2                 = {  7,  6, 2,-0.1000000000000069     };
    
    final double s0 = 13, s1 =  2, s2 = -3;
                
    final double[] res_minus_I           = {v1[0]-v2[0], v1[1]-v2[1], v1[2]-v2[2], v1[3]-v2[3]}; //  v1_I -  v2_I
    final double[] res_minus_scal_I      = {v1[0]-s0, v1[1]-s0, v1[2]-s0, v1[3]-s0}; //  v1_I -  13
    final double[] res_timesMinus_I      = {s1*v1[0]-v2[0], s1*v1[1]-v2[1], s1*v1[2]-v2[2], s1*v1[3]-v2[3]}; //  v1_I - 6v2
    final double[] res_minusTimes_I      = {v1[0]-s2*v2[0], v1[1]-s2*v2[1], v1[2]-s2*v2[2], v1[3]-s2*v2[3]}; // 6v1 -  v2_I
    final double[] res_timesMinustimes_I = {s1*v1[0]-s2*v2[0], s1*v1[1]-s2*v2[1], s1*v1[2]-s2*v2[2], s1*v1[3]-s2*v2[3]}; // 2v1 -(-3)v2_I
    
    // minus  and minusEquals (Vector - Vector) 
    assertArrayEquals(res_minus_I, minus(v1,v2), EPSILON);
    assertArrayEquals(res_minus_I, minusEquals(copy(v1), v2), EPSILON);
    
    //minus() and minusEquals (Vector - Skalar)
    assertArrayEquals(res_minus_scal_I, minus(v1,s0), EPSILON);
    assertArrayEquals(res_minus_scal_I, minusEquals(copy(v1), s0), EPSILON);
    
    // timesMinus() and timesMinusEquals() 
    assertArrayEquals(res_timesMinus_I, timesMinus(v1, s1, v2), EPSILON);
    assertArrayEquals(res_timesMinus_I, timesMinusEquals(copy(v1), s1, v2), EPSILON);
    
    // minusTimes() and minusTimesEquals()
    assertArrayEquals(res_minusTimes_I, minusTimes(v1, v2, s2), EPSILON);
    assertArrayEquals(res_minusTimes_I, minusTimesEquals(copy(v1), v2, s2), EPSILON);
    
    // timesMinustimes() and timesMinustimesEquals()
    assertArrayEquals(res_timesMinustimes_I, timesMinusTimes(v1, s1, v2, s2), EPSILON);
    assertArrayEquals(res_timesMinustimes_I, timesMinusTimesEquals(copy(v1), s1, v2, s2), 0);
    
    
    
    //TestcaseII
    final double delta = 1E-10;
    final double[] v3                 = { 0.17825, 32.546, 2958.3 };
    final double[] v4                 = { 0.82175, 67.454, 7041.7 };
    
                
    final double s00 = 0.92175, s3 =  1.5, s4 =  0.5;
                
    final double[] res_minus_II           = {v3[0]-v4[0], v3[1]-v4[1], v3[2]-v4[2]}; //  v1_II -  v2_II
    final double[] res_minus_scal_II      = {v3[0]-s00, v3[1]-s00, v3[2]-s00}; //  v1_II -  13
    final double[] res_timesMinus_II      = {s3*v3[0]-v4[0], s3*v3[1]-v4[1], s3*v3[2]-v4[2]}; //  v1_II - 6v2
    final double[] res_minusTimes_II      = {v3[0]-s4*v4[0], v3[1]-s4*v4[1], v3[2]-s4*v4[2]}; // 6v1 -  v2_II
    final double[] res_timesMinustimes_II = {s3*v3[0]-s4*v4[0], s3*v3[1]-s4*v4[1], s3*v3[2]-s4*v4[2]}; // 2v1 -(-3)v2_II

    // minus  and minusEquals (Vector - Vector) 
    assertArrayEquals(res_minus_II, minus(v3,v4), delta);
    assertArrayEquals(res_minus_II, minusEquals(copy(v3), v4), delta);
    
    //minus() and minusEquals (Vector - Skalar)
    assertArrayEquals(res_minus_scal_II, minus(v3,s00), delta);
    assertArrayEquals(res_minus_scal_II, minusEquals(copy(v3), s00), delta);
    
    // timesMinus() and timesMinusEquals() 
    assertArrayEquals(res_timesMinus_II, timesMinus(v3, s3, v4), delta);
    assertArrayEquals(res_timesMinus_II, timesMinusEquals(copy(v3), s3, v4), delta);
    
    // minusTimes() and minusTimesEquals()
    assertArrayEquals(res_minusTimes_II, minusTimes(v3, v4, s4), delta);
    assertArrayEquals(res_minusTimes_II, minusTimesEquals(copy(v3), v4, s4), delta);
    
    // timesMinustimes() and timesMinustimesEquals()
    assertArrayEquals(res_timesMinustimes_II, timesMinusTimes(v3, s3, v4, s4), delta);
    assertArrayEquals(res_timesMinustimes_II, timesMinusTimesEquals(copy(v3), s3, v4, s4), delta);
    
    
    // general testing
    final double[] v5                 = {1,2,3};
    final double[] v6                 = {4,5,6};              
    final double s5 =  2, s6 =  3;
    
    // checking that vector - same_vector is zero
    assertArrayEquals(new double[] {0,0,0,}, minus(v5, v5), 0.);
    // consistency check to plus methods
    assertArrayEquals(plus(v5, times(v6, -1)), minus(v5, v6), 0.);
    // consistency check within the minus methods
    assertArrayEquals(minus(v5, times(v6, s6)), minusTimes(v5, v6, s6), 0.);    
    assertArrayEquals(minus(times(v5, s5), v6), timesMinus(v5, s5, v6), 0.);
    assertArrayEquals(minus(times(v5, s5), times(v6, s6)), timesMinusTimes(v5, s5, v6, s6), 0.);
    
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
        {v1[0]*v2[0], v1[0]*v2[1], v1[0]*v2[2], v1[0]*v2[3]}, //
        {v1[1]*v2[0], v1[1]*v2[1], v1[1]*v2[2], v1[1]*v2[3]}, //
        {v1[2]*v2[0], v1[2]*v2[1], v1[2]*v2[2], v1[2]*v2[3]}, //
        {v1[3]*v2[0], v1[3]*v2[1], v1[3]*v2[2], v1[3]*v2[3]}  //
    };
    
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
   * <p>
   * We assert here that the angle method does what it did till now: <br> 
   * calculating the cosine of the angle between two vectors, 
   * where the smaller angle between those vectors is viewed.
   */
  @Test
  public void testAngle() {
    // TODO: Fix documentation for Angle method
    // Testscase II
    final double[] v1_I = {1,0};
    final double[] v2_I = {1,1};

    assertEquals(Math.cos(Math.PI/4), angle(v2_I, v1_I), EPSILON);
    assertEquals(Math.cos(Math.PI/4), angle(v1_I, v2_I), EPSILON);
    
    // set the origin, no change of data needed
    final double[] origin_I = {0,1};
    assertEquals(Math.cos(Math.PI/4), angle(v2_I, v1_I,  origin_I), EPSILON);
    assertEquals(Math.cos(Math.PI/4), angle(v1_I, v2_I,  origin_I), EPSILON);
    
    // Testscase II
    final double[] v1_II = {1,0};
    final double[] v2_II = {1, Math.tan(Math.PI/3)};

    assertEquals(Math.cos(Math.PI/3), angle(v2_II, v1_II), EPSILON);
    assertEquals(Math.cos(Math.PI/3), angle(v1_II, v2_II), EPSILON);
    
    // change the origin
    final double[] v3_II = {2,3};
    final double[] v4_II = {2, 3+Math.tan(Math.PI/3)};
    final double[] origin_II = {1,3};
    assertEquals(Math.cos(Math.PI/3), angle(v3_II, v4_II,  origin_II), EPSILON);
    assertEquals(Math.cos(Math.PI/3), angle(v4_II, v3_II,  origin_II), EPSILON);
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
   * Testing that correct Error is raised when dimension of the input data mismatch the needs of the method.
   */
  @Test
  public void testDimensionMismatch() {

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
