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
 * Unit test for LU decomposition.
 * 
 * @author Erich Schubert
 */
public final class LUDecompositionTest {
  @Test
  public void testJamaExample() {
    double[][] M = transpose(new double[][] { { 0., 2., 3. }, { 5., 6., 7. }, { 9., 10., 11. } });
    LUDecomposition lu = new LUDecomposition(M);
    final double[][] l = lu.getL(), u = lu.getU();
    checkTriangular(l, u);
    final double[][] B = times(l, u);
    // Rearrange
    final double[][] M2 = getMatrix(M, lu.getPivot(), 0, M.length);
    assertTrue("Not a proper decomposition.", almostEquals(M2, B, 1e-15));
  }

  @Test
  public void testJamaSolve() {
    double[][] s = { { 5., 8. }, { 6., 9. } };
    double[][] in = { { 13 }, { 15 } };
    double[][] sol = { { 1 }, { 1 } };
    double[][] o = new LUDecomposition(s).solve(in);
    assertTrue("Not solved.", almostEquals(sol, o, 1e-15));

    double[] in2 = { 13, 15 };
    double[] sol2 = { 1, 1 };
    double[] o2 = new LUDecomposition(s).solve(in2);
    assertTrue("Not solved.", almostEquals(sol2, o2, 1e-15));

    double[][] p = { { 4., 1., 1. }, { 1., 2., 3. }, { 1., 3., 6. } };
    double[][] o3 = new LUDecomposition(p).solve(unitMatrix(3));
    assertTrue("Not solved.", almostEquals(unitMatrix(3), times(p, o3), 1e-14));
  }

  @Test
  public void testWikipediaQR() {
    double[][] M = { //
        { 12, -51, 4 }, //
        { 6, 167, -68 }, //
        { -4, 24, -41 }//
    };
    LUDecomposition lu = new LUDecomposition(M);
    final double[][] l = lu.getL(), u = lu.getU();
    checkTriangular(l, u);
    final double[][] B = times(l, u);
    // Rearrange
    final double[][] M2 = getMatrix(M, lu.getPivot(), 0, M.length);
    assertTrue("Not a proper decomposition.", almostEquals(M2, B, 1e-15));
  }

  public void checkTriangular(double[][] l, double[][] u) {
    for(int row = 1; row < l.length; row++) {
      for(int col = row + 1; col < l[row].length; col++) {
        assertEquals(0., l[row][col], 0.);
      }
    }
    for(int row = 0; row < u.length; row++) {
      for(int col = 0; col < row; col++) {
        assertEquals(0., u[row][col], 0.);
      }
    }
  }
}
