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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

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
  protected final double[][] TESTMATRIX = {
      {1,2,3,4,5}, 
      {2,3,4,5,1}, 
      {3,4,5,1,2}, 
      {4,5,1,2,3} };
  
  /**
   * 1x1 Matrix testing for dimension mismatch testing.
   */
  public final double[][] DIMTESTMATRIX = {{1}};
  
  protected static final double EPSILON = 1E-15;
  
  // TODO: replace with import of VMathVectortest
  protected final double[] TESTVEC = {2,3,5,7,9};
  
  /**
   * Testing the Matrix plus operations of VMath class.
   */
  @Test
  public void testMatrixPLUS() {
    // TODO: implement plus(m1, m2); plusEquals(m1, m2); plusTimes(m1, m2, s2) ... as in VectorPLUS 
  }
  
  /**
   * Testing the Matrix minus operations of VMath class.
   */
  @Test
  public void testMatrixMINUS() {
    // TODO: implement minus(m1, m2) ... as in VectorMinus 
  }
  
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
    assertTrue((almostEquals(res_case1, out_case1, 0)));
    assertNotSame(m1, out_case1);
    
    
    // FIXME: Question: How many testcases needed
    
  }
  
  /**
   * Testing the Matrix,Scalar multiplication methods of VMath class. 
   */
  @Test
  public void testMatrixScalarMultiplication() {
    final double[][] m1 = TESTMATRIX;
    
    final double[][] m1_times_one_third = {
        {1/3*m1[0][0], 1/3*m1[0][1], 1/3*m1[0][2], 1/3*m1[0][3], 1/3*m1[0][4]},
        {1/3*m1[1][0], 1/3*m1[1][1], 1/3*m1[1][2], 1/3*m1[1][3], 1/3*m1[1][4]},
        {1/3*m1[2][0], 1/3*m1[2][1], 1/3*m1[2][2], 1/3*m1[2][3], 1/3*m1[2][4]},
        {1/3*m1[3][0], 1/3*m1[3][1], 1/3*m1[3][2], 1/3*m1[3][3], 1/3*m1[3][4]}};
    
    assertTrue(almostEquals(m1_times_one_third, times(m1, 1/3),EPSILON));

    assertTrue(almostEquals(m1_times_one_third, timesEquals(m1, 1/3),EPSILON));

    
    final double[][] m1_times_zero = {
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0}};

    assertTrue(Equals(m1_times_zero, times(m1, 0) ));
    assertTrue(Equals(m1_times_zero, timesEquals(m1, 0) ));
    
    assertTrue(Equals(m1, times(m1, 1)));
    assertTrue(Equals(m1, timesEquals(copy(m1), 1)));
  }
  
  /**
   * Testing the Matrix, Matrix multiplications methods of VMath class.
   */
  @Test
  public void testMatrixMatrixMultiplication() {
    //  test times(matrix, scalar); timesEquals(matrix, scalar)
    final double[][] m1 = TESTMATRIX;
    // TODO: implement transposed manually or assume transpose method to be correct
    final double[][] m1_t = transpose(m1);
    
    // check times(Matrix, id) = Matrix = times(id, Matrix) 
    assertTrue(Equals(m1, times(m1, unitMatrix(5) )));
    assertTrue(Equals(m1, times(unitMatrix(4), m1 )));
    
    // check transposeTimesTranspose(Matrix, id) = transpose(Matrix) = transposeTimesTranspose(id, Matrix) 
    assertTrue(Equals(m1_t, transposeTimesTranspose(m1, unitMatrix(4) )));
    assertTrue(Equals(m1_t, transposeTimesTranspose(unitMatrix(5), m1 )));
    
    final double[][] m1_times_m1transposed = { 
        {transposeTimes(m1[0], m1[0]), transposeTimes(m1[0], m1[1]), transposeTimes(m1[0], m1[2]), transposeTimes(m1[0], m1[3]) },
        {transposeTimes(m1[1], m1[0]), transposeTimes(m1[1], m1[1]), transposeTimes(m1[1], m1[2]), transposeTimes(m1[1], m1[3]) },
        {transposeTimes(m1[2], m1[0]), transposeTimes(m1[2], m1[1]), transposeTimes(m1[2], m1[2]), transposeTimes(m1[2], m1[3]) },
        {transposeTimes(m1[3], m1[0]), transposeTimes(m1[3], m1[1]), transposeTimes(m1[3], m1[2]), transposeTimes(m1[3], m1[3]) } };
    
    // check timesTranspose without not using a matrix methods times
    assertTrue(Equals(m1_times_m1transposed, timesTranspose(m1, m1) ));
    
    // check timesTranspose without not using a vector method transposeTimes
    // this is at the same time a test for the times method assuming the test before succeeded.
    assertTrue(Equals(times(m1, m1_t), timesTranspose(m1, m1) ));
    // and the following analog a test for the transposeTimesTranspose method
    assertTrue(Equals(transposeTimesTranspose(m1_t, m1), timesTranspose(m1, m1) ));
    
    
    final double[][] m1transposed_times_m1 = {
        {transposeTimes(m1_t[0], m1_t[0]), transposeTimes(m1_t[0], m1_t[1]), transposeTimes(m1_t[0], m1_t[2]), transposeTimes(m1_t[0], m1_t[3]), transposeTimes(m1_t[0], m1_t[4]) },
        {transposeTimes(m1_t[1], m1_t[0]), transposeTimes(m1_t[1], m1_t[1]), transposeTimes(m1_t[1], m1_t[2]), transposeTimes(m1_t[1], m1_t[3]), transposeTimes(m1_t[1], m1_t[4]) },
        {transposeTimes(m1_t[2], m1_t[0]), transposeTimes(m1_t[2], m1_t[1]), transposeTimes(m1_t[2], m1_t[2]), transposeTimes(m1_t[2], m1_t[3]), transposeTimes(m1_t[2], m1_t[4]) },
        {transposeTimes(m1_t[3], m1_t[0]), transposeTimes(m1_t[3], m1_t[1]), transposeTimes(m1_t[3], m1_t[2]), transposeTimes(m1_t[3], m1_t[3]), transposeTimes(m1_t[3], m1_t[4]) },
        {transposeTimes(m1_t[4], m1_t[0]), transposeTimes(m1_t[4], m1_t[1]), transposeTimes(m1_t[4], m1_t[2]), transposeTimes(m1_t[4], m1_t[3]), transposeTimes(m1_t[4], m1_t[4]) } };
 
    // check transposeTimes without not using a matrix methods times
    // without transpose and times
    assertTrue(Equals(m1transposed_times_m1, transposeTimes(m1, m1) ));
    
    // check transposeTimes without using a vector method timesTransposed
    // this is as well a test for the transposeTimesTranspose method assuming the test before succeeded.
    assertTrue(Equals(times(m1_t, m1), transposeTimes(m1, m1) ));
    // and the following analog a test for the transposeTimesTranspose method
    assertTrue(Equals(transposeTimesTranspose(m1, m1_t), transposeTimes(m1, m1) ));

    // TODO extra testcase for times and  transposedTimestransposed

    //    final double[][] m2 = {
    //        {1.21, 2000},
    //        {0   ,-1   },
    //        {7   , 0   } };
    //    TODO: Question one more testcase needed?
    //    final double[][] m3 = {
    //        {1.21, 2000, 0},
    //        {0   ,-1   , 7} };
  }
  
  /**
   * Testing the Vector, Matrix multiplications methods of VMath class. 
   */
  @Test
  public void testVectorMatrixMultiplication() {
    // TODO: implement Vector and Matrix multiplication & transposed.
    // done with explicit definition of the results due to nameing issues
    final double[][] M = {
        {1.21, 2000},
        {0   ,-1   },
        {7   , 0   } };
 
    // FIXME: replace or assume transpose to be correct.
    final double[][] M_t = transpose(M);
    
    final double delta = 1E-11;
    
    //  times(m1, v2)
    assertArrayEquals(M_t[0], times(M, unitVector(2, 0)) , delta);
    assertArrayEquals(M_t[1], times(M, unitVector(2, 1)) , delta);

    //  transposeTimes(m1, v2);
    assertArrayEquals(M[0], transposeTimes(M, unitVector(3, 0)) , delta);
    assertArrayEquals(M[1], transposeTimes(M, unitVector(3, 1)) , delta);
    assertArrayEquals(M[2], transposeTimes(M, unitVector(3, 2)) , delta);
//  transposeTimesTimes(a, B, c);
  //FIXME: Question: times(v1, m2) vs transposeTimes(v1, m2) dublicate or what ? --> answer on timesTranspose(v1, m2);
  //FIXME: transposed and times notation. 
  }
    
  //TODO: implement Matrix Ref class
  
  /**
   * Testing the initialization and setting methods of VMath class on Matrix's. 
   */
  @Test
  public void testSetMatrix() {
    // Set a Matrix or a part of a Matrix.
    // TODO: implement setCol(m1, c, column); setRow(m1, r, row);
    
    
    // TODO: implement setMatrix(m1, r, c, m2); setMatrix(m1, r0, r1, c, m2); setMatrix(m1, r, c0, c1, m2); setMatrix(m1, r0, r1, c0, c1, m2);
    

  }
  
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

    assertTrue(Equals(unitMatrix(5), m_unit));
    assertTrue(Equals(identity(5, 5), m_unit));
 
    // test identity with dimensions 3x5 and  5x3
    final double[][] m_identity3x5 = {{1,0,0,0,0},
                                      {0,1,0,0,0},
                                      {0,0,1,0,0}};

    // TODO: implement Dimension Missmatch probably in extra class

    assertTrue(Equals(identity(3, 5), m_identity3x5));   
    assertTrue(Equals(identity(5, 3), transpose(m_identity3x5)));  
     
  }
  
  /**
   * Testing the diagnoal method of VMath class.
   */
  @Test
  public void testDiagnoal() {
    final double[][] m_diag = {{TESTVEC[0],0,0,0,0},
                               {0,TESTVEC[1],0,0,0},
                               {0,0,TESTVEC[2],0,0},
                               {0,0,0,TESTVEC[3],0},
                               {0,0,0,0,TESTVEC[4]}};
    
    assertTrue(Equals(diagonal(TESTVEC), m_diag));
  }
  
  /**
   * Testing the GET method of VMath class.
   */
  @Test
  public void testGetMatrix() {
    // Get a Submatix.
    // TODO: implement getMatrix(m1, r, c); getMatrix(m1, r0, r1, c); getMatrix(m1, r, c0, c1); getMatrix(m1, r0, r1, c0, c1);
    
    // Get colums of properties of am Matrix.
    // TODO: implement getCol(m1, col); getRow(m1, r)
    // TODO: implement getRowDimensionality(m1);    getColumnDimensionality(m1);
    // TODO: implement getDiagonal(m1);
    
  }
  
  /**
   * Testing the _solve method of VMath class.
   */
  @Test
  public void testSolve() {
    // TODO: implement solve(A, B);
  }
  
  /**
   * Testing the _appendColums method of VMath class.
   */
  @Test
  public void testAppendColums() {
    // TODO: implement appendColumns(m1, m2);
  }
  
  /**
   * Testing the Matrix_project method of VMath class.
   */
  @Test
  public void testProject() {
    // TODO: implement project(v1, m2)
  }

  /**
   * Testing the inverse method of VMath class.
   */
  @Test
  public void testinverse() {
    // TODO: implement inverse(elements)
  }
  
  // TODO: implement Dimension mismatch

}

// TODO: Have a look a normalize options of VMathclass