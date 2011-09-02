package de.lmu.ifi.dbs.elki.math;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
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

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

public class TestMatrix implements JUnit4Test {

  @Test
  public void testTransposedOperations() {
    for(int i = 0; i < 100; i++) {
      randomizedTransposedTest();
      randomizedTestAsymmetric();
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
  
  private void randomizedTestAsymmetric() {
    Random r = new Random();
    int dim1 = r.nextInt(30) + 10;
    int dim2 = r.nextInt(30) + 10;
    int dim3 = r.nextInt(30) + 10;
    Matrix A = new Matrix(dim1, dim2);
    Matrix B = new Matrix(dim2, dim3);
    for(int i = 0; i < dim1; i++) {
      for(int j = 0; j < dim2; j++) {
        A.set(i, j, (r.nextDouble() - .5) * 10);
      }
    }
    for(int i = 0; i < dim2; i++) {
      for(int j = 0; j < dim3; j++) {
        B.set(i, j, (r.nextDouble() - .5) * 10);
      }
    }

    Matrix A_B = A.times(B);
    Matrix BT_AT = B.transpose().times(A.transpose());
    Matrix BT_AT2 = B.transposeTimesTranspose(A);
    org.junit.Assert.assertTrue("B.transposeTimesTranspose(A) does not equal (A.times(B)).transpose", A_B.transpose().almostEquals(BT_AT));
    org.junit.Assert.assertTrue("B.transposeTimesTranspose(A) does not equal B.transpose.times(A.transpose)", BT_AT2.almostEquals(BT_AT));
  }
}