/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for QR decomposition.
 * 
 * @author Erich Schubert
 */
public final class QRDecompositionTest {
  @Test
  public void testJamaExample() {
    double[][] M = transpose(new double[][] { { 1., 2., 3., 4. }, { 5., 6., 7., 8. }, { 9., 10., 11., 12. } });
    QRDecomposition qr = new QRDecomposition(M);
    final double[][] q = qr.getQ(), r = qr.getR();
    assertTrue(almostEquals(unitMatrix(q[0].length), transposeTimes(q, q)));
    checkTriangular(r);
    assertTrue("Not a proper decomposition.", almostEquals(M, times(q, r), 1e-14));
  }

  @Test
  public void testJamaSolve() {
    double[][] s = { { 5., 8. }, { 6., 9. } };
    double[][] in = { { 13 }, { 15 } };
    double[][] sol = { { 1 }, { 1 } };
    double[][] o = new QRDecomposition(s).solve(in);
    assertTrue("Not solved.", almostEquals(sol, o, 1e-14));

    double[][] p = { { 4., 1., 1. }, { 1., 2., 3. }, { 1., 3., 6. } };
    double[][] o2 = new QRDecomposition(p).solve(unitMatrix(3));
    assertTrue("Not solved.", almostEquals(unitMatrix(3), times(p, o2), 1e-14));
  }

  @Test
  public void testWikipedia() {
    double[][] m = { //
        { 12, -51, 4 }, //
        { 6, 167, -68 }, //
        { -4, 24, -41 }//
    };
    QRDecomposition qr = new QRDecomposition(m);
    double[][] q = qr.getQ(), r = qr.getR();

    // Check that Q^T Q = Unit, i.e. rotation factor.
    assertTrue(almostEquals(unitMatrix(q[0].length), transposeTimes(q, q)));
    checkTriangular(r);
    assertTrue("Not a proper decomposition.", almostEquals(m, times(q, r), 1e-13));
  }

  public void checkTriangular(double[][] r) {
    for(int row = 1; row < r.length; row++) {
      for(int col = 0; col < row; col++) {
        assertEquals(0., r[row][col], 0.);
      }
    }
  }

  @Test
  public void testRank4() {
    double delta = 1e-14;
    double[][] m = transpose(new double[][] { //
        { 1, 1, 1, 1 + delta, 1, 1 }, //
        { 1, 1, 1, delta, 0, 0 }, //
        { 0, 0, 0, 1, 1, 1 }, //
        { 1, 0, 0, 1, 0, 0 }, //
        { 0, 0, 1, 0, 0, 1 } //
    });

    QRDecomposition qr = new QRDecomposition(m);
    double[][] q = qr.getQ(), r = qr.getR();
    assertTrue(almostEquals(unitMatrix(q[0].length), transposeTimes(q, q)));
    checkTriangular(r);
    assertTrue("Not a proper decomposition.", almostEquals(m, times(q, r), 1e-14));
    assertEquals("Rank not as expected", 4, qr.rank(1e-14));
  }
}
