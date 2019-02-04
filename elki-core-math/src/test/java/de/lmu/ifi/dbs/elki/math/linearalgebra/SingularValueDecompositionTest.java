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

import static org.junit.Assert.*;

import org.junit.Test;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

/**
 * Simple unit test for singular value decomposition.
 *
 * Copied from Jama, please contribute a more complex test example!
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SingularValueDecompositionTest {
  @Test
  public void testJamaExample() {
    double[][] M = transpose(new double[][] { { 1., 2., 3., 4. }, { 5., 6., 7., 8. }, { 9., 10., 11., 12. } });
    SingularValueDecomposition svd = new SingularValueDecomposition(M);
    // M = U * S * V^T
    double[][] M2 = times(svd.getU(), timesTranspose(svd.getS(), svd.getV()));
    assertTrue("Not a proper decomposition.", almostEquals(M, M2, 1e-14));
  }

  @Test
  public void testJamaCond() {
    double[][] condmat = { { 1., 3. }, { 7., 9. } };
    SingularValueDecomposition svd = new SingularValueDecomposition(condmat);
    double[] singularvalues = svd.getSingularValues();
    double cond = svd.cond();
    double check = singularvalues[0] / singularvalues[Math.min(getRowDimensionality(condmat), getColumnDimensionality(condmat)) - 1];
    assertEquals("Matrix condition.", cond, check, 0.);
  }
}
