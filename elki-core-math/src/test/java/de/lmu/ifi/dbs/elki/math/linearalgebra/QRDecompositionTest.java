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
  public void testRank5() {
    double delta = 1e-7;
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
    assertEquals(5, qr.rank());
  }
}
