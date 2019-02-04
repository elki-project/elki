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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Unit test for Mahalanobis distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MahalanobisDistanceFunctionTest extends AbstractDistanceFunctionTest {
  @Test
  public void testEuclideanConsistency() {
    double[][] data = TOY_VECTORS; // TODO: use data with more dimensions.
    final int dim = data[0].length;
    double[][] weights = VMath.identity(dim, dim);
    // TODO: No builder yet.
    MahalanobisDistanceFunction dist = new MahalanobisDistanceFunction(weights);
    MahalanobisDistanceFunction dis2 = new MahalanobisDistanceFunction(VMath.times(weights, 4));
    EuclideanDistanceFunction ref = EuclideanDistanceFunction.STATIC;
    for(int i = 0; i < data.length; i++) {
      final DoubleVector vi = DoubleVector.wrap(data[i]);
      for(int j = 0; j < data.length; j++) {
        final DoubleVector vj = DoubleVector.wrap(data[j]);
        assertEquals("Not consistent at " + i + "," + j, ref.distance(vi, vj), dist.distance(vi, vj), 1e-15);
        assertEquals("Not consistent at " + i + "," + j, ref.distance(vi, vj) * 2, dis2.distance(vi, vj), 1e-15);
      }
    }
    assertTrue("Equals not recreatable.", dist.equals(new MahalanobisDistanceFunction(VMath.copy(weights))));
    basicChecks(dist);
  }
}
