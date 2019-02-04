/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.almostEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.diagonal;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minus;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.normF;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesTranspose;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transpose;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Simple unit test for eigenvalue decomposition.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class EigenvalueDecompositionTest {
  @Test
  public void testToyExample() {
    double[][] evs = { { 0.5, MathUtil.SQRT3 / 2 }, { -MathUtil.SQRT3 / 2, 0.5 } };
    double[][] lam = { { 4, 0 }, { 0, 9 } }, lam2 = { { 9, 0 }, { 0, 4 } };
    testBasics(timesTranspose(times(evs, lam), evs));
    testBasics(timesTranspose(times(evs, lam2), evs));
  }

  private EigenvalueDecomposition testBasics(double[][] s) {
    EigenvalueDecomposition ev = new EigenvalueDecomposition(s);
    double[][] evec = transpose(ev.getV()); // Eigenvectors are in transpose!
    double[] eval = ev.getRealEigenvalues();
    double[][] diag = ev.getD();
    assertTrue("Diagonal does not agree with eigenvalues.", almostEquals(diagonal(eval), diag, 1e-15));

    // Try reconstruction of the original matrix:
    // Note that we transposed V above!
    double[][] r = times(transposeTimes(evec, diag), evec);
    assertTrue("Not a proper decomposition.", almostEquals(s, r, 0.));

    for(int i = 0; i < eval.length; i++) {
      assertTrue("Negative eigenvalue.", eval[i] >= 0);
      // Check that the matrix multiplication and eigenvalue do the same thing.
      assertTrue("Not an eigenvector.", almostEquals(times(evec[i], eval[i]), times(s, evec[i])));
    }
    return ev;
  }

  /**
   * Test added in Jama 1.0.3, causing an infinite loop.
   */
  @Test(timeout = 60000)
  public void testJama103() {
    double[][] badeigs = { { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 1 }, { 0, 0, 0, 1, 0 }, { 1, 1, 0, 0, 1 }, { 1, 0, 1, 0, 1 } };
    new EigenvalueDecomposition(badeigs);
  }

  @Test
  public void testAsymmetric() {
    double[][] a = transpose(new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
    EigenvalueDecomposition ev = new EigenvalueDecomposition(a);
    double[][] v = ev.getV(), d = ev.getD();
    assertEquals("Asymmetric decomposition", 0., normF(minus(times(a, v), times(v, d))), 1e-13);
  }
}
