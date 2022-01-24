/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
 * ELKI Development Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.math.linearalgebra;

import static org.junit.Assert.*;

import java.util.Random;

import static elki.math.linearalgebra.VMath.copy;
import org.junit.Test;

import elki.math.linearalgebra.LOBPCG.MatrixAdapter;
import elki.math.linearalgebra.pca.EigenPair;
import elki.math.linearalgebra.LOBPCG.DistanceArrayMatrixAdaper;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.optionhandling.ParameterException;

/**
 * Test the LOBPCG class
 * 
 * @author Robert Gehde
 *
 */
public class LOBPCGTest {
  private static double[][] a3x4 = new double[][] { { 9, 4, 5, 3 }, { 7, 1, 4, 8 }, { 1, 2, 1, 3 } };

  private static double[][] b4x3 = new double[][] { { 7, 6, 5 }, { 2, 4, 6 }, { 9, 1, 1 }, { 5, 4, 5 } };

  private static double[][] c4x4 = new double[][] { { 2, 8, 5, 7 }, { 1, 7, 4, 5 }, { 1, 9, 2, 7 }, { 9, 1, 1, 4 } };

  private static double[][] d3x4 = new double[][] { { 4, 5, 3, 6 }, { 8, 6, 1, 2 }, { 4, 8, 8, 7 } };

  private static double[][] e3x3 = new double[][] { { 8, 2, 9 }, { 7, 6, 4 }, { 3, 4, 5 } };

  private static double[][] f4x4 = new double[][] { { 6, 2, 6, 6 }, { 6, 6, 3, 6 }, { 8, 8, 1, 7 }, { 7, 2, 4, 9 } };

  private static double[][] g2x2 = new double[][] { { 9, 4 }, { 6, 8 } };

  private static double[] diag4 = new double[] { 4, 1, 1, 8 };

  @Test
  public void testLOBPCG() {
    double[][] data = new double[][] { { 2, 8, 5, 7 }, { 1, 7, 4, 5 }, { 1, 9, 2, 7 }, { 9, 1, 1, 4 } };
    double[][] oga = VMath.transposeTimes(data, data);
    // VMath.normalizeColumns(oga);
    MatrixAdapter adapter = new DistanceArrayMatrixAdaper(oga, VMath.inverse(oga));
    try {
      LOBPCG lob = new LOBPCG(4, adapter, .001);
      double[][] base = new double[][] { { 1, 0, 0, 0 }, { 0, 1, 0, 0 }, { 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
      EigenPair[] eigenpairs = lob.calculateEigenvalues(base, null, 100);
      EigenvalueDecomposition eigendecomp = new EigenvalueDecomposition(oga);
      double[] shouldVal = eigendecomp.getRealEigenvalues();
      // check vectors
      for(int j = 0; j < base[0].length; j++) {
        double isVal = eigenpairs[j].getEigenvalue();
        double[] isVec = eigenpairs[j].getEigenvector();
        assertEquals(shouldVal[j], isVal, 1e-10);
        checkEigen(oga, isVec, isVal, 1e-10);
      }
    }
    catch(ParameterException e) {
      elki.logging.LoggingUtil.exception(e);
    }
  }

  @Test
  public void testLOBPCGPart() {
    double[][] data = new double[][] { { 2, 8, 5, 7 }, { 1, 7, 4, 5 }, { 1, 9, 2, 7 }, { 9, 1, 1, 4 } };
    double[][] oga = VMath.transposeTimes(data, data);
    double[][] cond = new double[4][2];
    // VMath.normalizeColumns(oga);
    MatrixAdapter adapter = new DistanceArrayMatrixAdaper(oga, VMath.inverse(oga));
    try {
      LOBPCG lob = new LOBPCG(4, adapter, .001);
      double[][] base = { { 1, 0 }, { 0, 1 }, { 0, 0 }, { 0, 0 } };
      EigenPair[] eigenpairs = lob.calculateEigenvalues(base, null, 100);
      VMath.setCol(cond, 0, eigenpairs[0].getEigenvector());
      VMath.setCol(cond, 1, eigenpairs[1].getEigenvector());
      EigenvalueDecomposition eigendecomp = new EigenvalueDecomposition(oga);
      double[] shouldVal = eigendecomp.getRealEigenvalues();
      // check vectors
      for(int j = 0; j < base[0].length; j++) {
        double isVal = eigenpairs[j].getEigenvalue();
        double[] isVec = eigenpairs[j].getEigenvector();
        assertEquals(shouldVal[j], isVal, 1e-10);
        checkEigen(oga, isVec, isVal, 1e-10);
      }
    }
    catch(ParameterException e) {
      elki.logging.LoggingUtil.exception(e);
    }
    try {
      LOBPCG lob = new LOBPCG(4, adapter, .001);
      double[][] base = { { 1, 0 }, { 0, 1 }, { 0, 0 }, { 0, 0 } };
      EigenPair[] eigenpairs = lob.calculateEigenvalues(base, cond, 100);
      EigenvalueDecomposition eigendecomp = new EigenvalueDecomposition(oga);
      double[] shouldVal = eigendecomp.getRealEigenvalues();
      // check vectors
      for(int j = 3; j < 4; j++) {
        double isVal = eigenpairs[j - 2].getEigenvalue();
        double[] isVec = eigenpairs[j - 2].getEigenvector();
        assertEquals(shouldVal[j], isVal, 1e-10);
        checkEigen(oga, isVec, isVal, 1e-10);
      }
    }
    catch(ParameterException e) {
      elki.logging.LoggingUtil.exception(e);
    }
  }

//   @Test
  public void testLOBPCGGramRandom() {
    Random rng = new Random(13);
    int tests = 100;
    for(int i = 0; i < tests; i++) {
      // generate random points
      double[][] data = new double[3][5];
      for(int j = 0; j < data.length; j++) {
        for(int k = 0; k < data[j].length; k++) {
          data[j][k] = rng.nextDouble();
        }
      }
      double[][] oga = VMath.transposeTimes(data, data);

      MatrixAdapter adapter = null;
      try {
        adapter = new DistanceArrayMatrixAdaper(oga, VMath.inverse(oga));
      }
      catch(ArithmeticException e) {
        i--;
        continue;
      }
      try {
        LOBPCG lob = new LOBPCG(5, adapter, .001);
        // double[][] base = new double[][] { { 0, 2, 0, 0, 0 }, { 0, 0, .5, 0,
        // 0 }, { 0, 0, 0, 0, 8 }, { 2, 0, 0, 0, 0 }, { 0, 0, 0, 1, 0 } };
        double[][] base = new double[][] { { 0, 2 }, { 0, 0 }, { 0, 0 }, { 2, 0 }, { 0, 0 } };
        // double[][] base = new double[][] { { 1 }, { 0 }, { 0 }, { 0 }, { 0 }
        // };
        EigenvalueDecomposition eigendecomp = new EigenvalueDecomposition(oga);
        double[] shouldVal = eigendecomp.getRealEigenvalues();
        EigenPair[] eigenpairs = lob.calculateEigenvalues(base, null, 100);
        // check vectors
        for(int j = 0; j < base[0].length; j++) {
          double isVal = eigenpairs[j].getEigenvalue();
          double[] isVec = eigenpairs[j].getEigenvector();
          assertEquals(shouldVal[j], isVal, 1e-10);
          checkEigen(oga, isVec, isVal, 1e-10);
        }
      }
      catch(ParameterException e) {
        elki.logging.LoggingUtil.exception(e);
      }
    }
  }

  @Test
  public void testAdapter() {
    double[][] oga = new double[][] { { 3, 2, 4 }, { 2, 0, 2 }, { 4, 2, 3 } };
    MatrixAdapter adapter = new DistanceArrayMatrixAdaper(oga, VMath.inverse(oga));

    // ax
    double[][] should = { { 45, 22, 27, 37 }, { 20, 12, 12, 12 }, { 53, 24, 31, 37 } };
    double[][] is = new double[3][4];
    adapter.ax(a3x4, is);
    check(should, is, 1e-10);
    // tx
    should = new double[][] { { -2.25, -0.75, -1, 2 }, { -3.625, 0.625, -2, -5.5 }, { 5.75, 1.25, 3, 2 } };
    is = copy(a3x4);
    adapter.tx(is, is);
    check(should, is, 1e-10);

    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);

    // ax reduced
    should = new double[][] { { 45, 0, 27, 0 }, { 20, 0, 12, 0 }, { 53, 0, 31, 0 } };
    is = new double[3][4];
    adapter.axReduced(a3x4, is, indices);
    check(should, is, 1e-10);
    // tx reduced
    should = new double[][] { { -2.25, 4, -1, 3 }, { -3.625, 1, -2, 8 }, { 5.75, 2, 3, 3 } };
    is = copy(a3x4);
    adapter.txReduced(is, is, indices);
    check(should, is, 1e-10);

  }

  @Test
  public void testBuildGramMatrices() {
    double[][] oga = new double[][] { { 3, 2, 4, 1 }, { 2, 0, 2, 3 }, { 4, 2, 3, 4 }, { 1, 3, 4, 2 } };
    MatrixAdapter adapter = new DistanceArrayMatrixAdaper(oga, VMath.inverse(oga));

    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 1);

    try {
      LOBPCG lob = new LOBPCG(4, adapter, 1e-3);
      final int cursize = 2;

      double[] lambdadiag = new double[] { 4.0, 0.3, 0.2 };
      double[][] x = new double[][] { { 3, 2, 1 }, { 2, 0, 1 }, { 1, 3, 4 }, { 3, 2.4, 1.2 } };
      double[][] w = new double[][] { { 1, 0.3, 0.1 }, { 4, 2, 1 }, { 0.5, 0.2, 0.1 }, { 1.2, 0.3, 0.4 } };
      double[][] p = new double[][] { { 1.1, 1.8, 2.2 }, { 1, 2, 3 }, { 3, 1, 2 }, { 3.2, 5.9, 2.1 } };
      double[][] aw = new double[4][3];
      adapter.ax(w, aw);
      double[][] ap = new double[4][3];
      adapter.ax(p, ap);

      double[][] cx = new double[3][3];
      double[][] cw = new double[3][3];
      double[][] cp = new double[3][3];
      // first iteration
      // gram
      int it = 0;
      double[][] gramA = new double[5][5], gramB = new double[5][5];

      lob.buildGramMatrices(gramA, gramB, lambdadiag, x, w, aw, p, ap, it, cursize, indices);

      double[][] gramAShould = new double[][] { //
          { 4.0, 0, 0, 126.3, 51.9 }, //
          { 0, 0.3, 0, 125.06, 51.48 }, //
          { 0, 0, 0.2, 114.88, 45.14 }, //
          { 126.3, 125.06, 114.88, 70.63, 26.34 }, //
          { 51.9, 51.48, 45.14, 26.34, 9.31 } }; //
      double[][] gramBShould = new double[][] { //
          { 1, 0, 0, 15.1, 6 }, //
          { 0, 1, 0, 6.38, 1.92 }, //
          { 0, 0, 1, 8.44, 3.46 }, //
          { 15.1, 6.38, 8.44, 1, 0 }, //
          { 6, 1.92, 3.46, 0, 1 } }; //

      check(gramAShould, gramA, 1e-10);
      check(gramBShould, gramB, 1e-10);

      // partition

      lob.partition(gramA, cx, cw, indices, cursize, 3);
      double[][] cxshould = new double[][] { { 4, 0, 0 }, { 0, 0.3, 0 }, { 0, 0, 0.2 } };
      double[][] cwshould = new double[][] { { 126.3, 125.06, 114.88 }, { 51.9, 51.48, 45.14 }, { 0, 0, 0 } };

      check(cxshould, cx, 1e-10);
      check(cwshould, cw, 1e-10);

      // other iterations
      // gram
      it = 1;
      gramA = new double[7][7];
      gramB = new double[7][7];

      lob.buildGramMatrices(gramA, gramB, lambdadiag, x, w, aw, p, ap, it, cursize, indices);

      gramAShould = new double[][] { //
          { 4.0, 0, 0, 126.3, 51.9, 192.8, 213.1 }, //
          { 0, 0.3, 0, 125.06, 51.48, 179.6, 208.64 }, //
          { 0, 0, 0.2, 114.88, 45.14, 178.1, 222.12 }, //
          { 126.3, 125.06, 114.88, 70.63, 26.34, 132.8, 159.72 }, //
          { 51.9, 51.48, 45.14, 26.34, 9.31, 54.14, 67.03 }, //
          { 192.8, 179.6, 178.1, 132.8, 54.14, 196.95, 233.45 }, //
          { 213.1, 208.64, 222.12, 159.72, 67.03, 233.45, 258.38 } }; //
      gramBShould = new double[][] { //
          { 1, 0, 0, 15.1, 6, 17.9, 28.1 }, //
          { 0, 1, 0, 6.38, 1.92, 18.88, 20.76 }, //
          { 0, 0, 1, 8.44, 3.46, 17.94, 14.88 }, //
          { 15.1, 6.38, 8.44, 1, 0, 10.44, 17.38 }, //
          { 6, 1.92, 3.46, 0, 1, 3.89, 6.51 }, //
          { 17.9, 18.88, 17.94, 10.44, 3.89, 1, 0 }, //
          { 28.1, 20.76, 14.88, 17.38, 6.51, 0, 1 } }; //

      check(gramAShould, gramA, 1e-10);
      check(gramBShould, gramB, 1e-10);

      // partition

      lob.partition(gramA, cx, cw, cp, indices, cursize, 3);
      double[][] cpshould = new double[][] { { 192.8, 179.6, 178.1 }, { 213.1, 208.64, 222.12 }, { 0, 0, 0 } };

      check(cxshould, cx, 1e-10);
      check(cwshould, cw, 1e-10);
      check(cpshould, cp, 1e-10);
    }
    catch(ParameterException e) {
      fail("Error: " + e);
    }
  }

  @Test
  public void testOrthonormalize() {
    double[][] oga = copy(b4x3);
    LOBPCG.orthonormalize(oga, new double[3][3], null);
    for(int i = 0; i < oga[0].length; i++) {
      assertEquals(1, VMath.dot(VMath.getCol(oga, i), VMath.getCol(oga, i)), 1e-10);
      for(int j = i + 1; j < oga[0].length; j++) {
        assertEquals(0, VMath.dot(VMath.getCol(oga, i), VMath.getCol(oga, j)), 1e-10);
      }
    }
  }

  @Test
  public void testTimesEqualsSecond() {
    // fail("Not yet implemented");
    double[][] should = new double[][] { { 110, 77, 98 }, { 82, 58, 76 }, { 78, 72, 96 }, { 94, 75, 72 } };
    check(should, LOBPCG.timesEqualsSecond(c4x4, copy(b4x3)), 1e-10);
  }

  @Test
  public void testTimesEqualsFirst() {
    // fail("Not yet implemented");
    double[][] should = new double[][] { { 54, 148, 74, 130 }, { 91, 107, 55, 114 }, { 32, 34, 18, 36 } };
    check(should, LOBPCG.timesEqualsFirst(copy(a3x4), c4x4), 1e-10);
  }

  @Test
  public void testMinusTimesEquals() {
    // fail("Not yet implemented");
    double[][] should = new double[][] { { -108, -36, -59, -77 }, { -51, -17, -28, -51 }, { -88, -30, -48, -31 }, { -69, -33, -45, -58 } };
    check(should, LOBPCG.minusTimesEquals(copy(c4x4), b4x3, a3x4), 1e-10);
  }

  @Test
  public void testReducedMinusReducedTimesDiagReduced() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { -7, 4, 2, 3 }, { -25, 1, 3, 8 }, { -15, 2, -7, 3 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedMinusReducedTimesDiagReduced(copy(a3x4), d3x4, diag4, copy(a3x4), indices), 1e-10);
  }

  @Test
  public void testReducedTransposeTimesReducedIntoCompactSymmetric() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 0, 0, 96, 42 }, { 0, 0, 56, 27 }, { 96, 56, 0, 0 }, { 42, 27, 0, 0 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedTransposeTimesReducedIntoCompactSymmetric(a3x4, d3x4, new double[4][4], indices, 0, 2), 1e-10);
  }

  @Test
  public void testTransposeTimesReducedIntoCompactSymmetric() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 0, 0, 0, 0, 96, 42 }, { 0, 0, 0, 0, 32, 29 }, { 0, 0, 0, 0, 56, 27 }, { 0, 0, 0, 0, 88, 41 }, { 96, 32, 56, 88, 0, 0 }, { 42, 29, 27, 41, 0, 0 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.transposeTimesReducedIntoCompactSymmetric(a3x4, d3x4, new double[6][6], indices, 0, 4), 1e-10);
  }

  @Test
  public void testTransposeTimesReducedIntoReduced() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 96, 0, 42, 0 }, { 32, 0, 29, 0 }, { 56, 0, 27, 0 }, { 88, 0, 41, 0 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.transposeTimesReducedIntoReduced(a3x4, d3x4, new double[4][4], indices), 1e-10);
  }

  @Test
  public void testTimesReducedEqualsSecondReduced() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 84, 5, 98, 6 }, { 92, 6, 59, 2 }, { 64, 8, 53, 7 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.timesReducedEqualsSecondReduced(e3x3, copy(d3x4), indices), 1e-10);
  }

  @Test
  public void testReducedMinusTimesReducedEqualsReduced() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { -75, 4, -93, 3 }, { -85, 1, -55, 8 }, { -63, 2, -52, 3 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedMinusTimesReducedEqualsReduced(copy(a3x4), e3x3, d3x4, indices), 1e-10);
  }

  @Test
  public void testReducedTimesPlusReducedTimesEqualsSecond() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 71, 149, 82, 143 }, { 74, 116, 92, 132 }, { 91, 89, 39, 94 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedTimesPlusReducedTimesEqualsSecond(a3x4, c4x4, copy(d3x4), f4x4, indices), 1e-10);
  }

  @Test
  public void testTimesPlusEqualsFirst() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 58, 153, 77, 136 }, { 99, 113, 56, 116 }, { 36, 42, 26, 43 } };
    check(should, LOBPCG.timesPlusEqualsFirst(copy(a3x4), c4x4, d3x4), 1e-10);
  }

  @Test
  public void testReducedTimesRowReduced() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 23, 117, 55, 98 }, { 18, 92, 43, 77 }, { 3, 17, 7, 14 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedTimesRowReduced(a3x4, c4x4, new double[3][4], indices), 1e-10);
  }

  @Test
  public void testReducedTimesCompactEqualsFirstReduced() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 111, 4, 76, 3 }, { 87, 1, 60, 8 }, { 15, 2, 12, 3 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedTimesCompactEqualsFirstReduced(copy(a3x4), g2x2, indices), 1e-10);
  }

  @Test
  public void testReducedTransposeTimesReducedIntoCompact() {
    // reduced lines filled with a to check access
    double[][] should = new double[][] { { 131, 74 }, { 74, 42 } };
    long indices[] = new long[] { 0l };
    BitsUtil.setI(indices, 0);
    BitsUtil.setI(indices, 2);
    check(should, LOBPCG.reducedTransposeTimesReducedIntoCompact(a3x4, new double[2][2], indices), 1e-10);
  }

  @Test
  public void testExtractCol() {
    double[][] oga = new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 7 } };
    double[] should = new double[] { 2, 5, 8 };
    double[] is = VMath.getCol(oga, 1);
    for(int i = 0; i < is.length; i++) {
      assertEquals("error at " + i, should[i], is[i], 1e-15);
    }
  }

  private void checkEigen(double[][] a, double[] v, double e, double tol) {
    double[] lhs = VMath.times(a, v);
    double[] rhs = VMath.times(v, e);
    for(int i = 0; i < lhs.length; i++) {
      assertEquals(lhs[i], rhs[i], tol);
    }
  }

  /**
   * checks every array field
   * 
   * @param should
   * @param is
   * @param tol TODO
   */
  private void check(double[][] should, double[][] is, double tol) {
    assertEquals(should.length, is.length);
    assertEquals(should[0].length, is[0].length);
    for(int i = 0; i < is.length; i++) {
      for(int j = 0; j < is[0].length; j++) {
        assertEquals("error at " + i + "|" + j, should[i][j], is[i][j], tol);
      }
    }
  }
}
