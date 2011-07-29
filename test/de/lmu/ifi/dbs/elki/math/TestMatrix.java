package de.lmu.ifi.dbs.elki.math;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

public class TestMatrix implements JUnit4Test {

  @Test
  public void testTransposedOperations() {
    for(int i = 0; i < 100; i++) {
      randomizedTransposedTest();
    }
  }

  private void randomizedTransposedTest() {
    Random r = new Random();
    int dim = r.nextInt(30) + 10;
    Matrix A = new Matrix(dim, dim);
    Matrix B = new Matrix(dim, dim);
    for(int i = 0; i < dim; i++) {
      for(int j = 0; j < dim; j++) {
        A.set(i, j, (r.nextDouble() - .5) * 10);
        B.set(i, j, (r.nextDouble() - .5) * 10);
      }
    }

    Matrix AT_B = A.transpose().times(B);
    org.junit.Assert.assertTrue("A.transposeTimes(B) does not equal A.transpose.times(B)", A.transposeTimes(B).almostEquals(AT_B));
    Matrix A_BT = A.times(B.transpose());
    org.junit.Assert.assertTrue("A.timesTranspose(B) does not equal A.times(B.transpose)", A.timesTranspose(B).almostEquals(A_BT));
    org.junit.Assert.assertTrue("Usually (!) AT_B != A_BT!", !AT_B.almostEquals(A_BT));
    Matrix AT_BT = A.transpose().times(B.transpose());
    org.junit.Assert.assertTrue("A.transposeTimesTranspose(B) does not equal (B.times(A)).transpose", B.times(A).transpose().almostEquals(AT_BT));
    org.junit.Assert.assertTrue("A.transposeTimesTranspose(B) does not equal A.transpose.times(B.transpose)", A.transposeTimesTranspose(B).almostEquals(AT_BT));
  }
}
