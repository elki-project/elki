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
package elki.distance.distancefunction.histogram;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.distance.distancefunction.AbstractDistanceTest;
import elki.distance.distancefunction.minkowski.ManhattanDistance;
import elki.math.linearalgebra.VMath;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for histogram match distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HistogramMatchDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    HistogramMatchDistance dist = new ELKIBuilder<>(HistogramMatchDistance.class).build();
    basicChecks(dist);
    nonnegativeSpatialConsistency(dist);
  }

  /**
   * Vectors of L1 norm 1 should yield the same result as Manhattan distance.
   */
  @Test
  public void testConsistencyManhattan() {
    HistogramMatchDistance dist = HistogramMatchDistance.STATIC;
    ManhattanDistance mdist = ManhattanDistance.STATIC;
    final int numv = 10, dim = 5;
    Random rnd = new Random(0L);
    DoubleVector[] vs = new DoubleVector[numv];
    for(int i = 0; i < numv; i++) {
      double sum = 0;
      double[] data = new double[dim];
      for(int j = 0; j < dim; j++) {
        sum += data[j] = rnd.nextDouble();
      }
      VMath.timesEquals(data, 1. / sum);
      vs[i] = DoubleVector.wrap(data);
    }

    for(int i = 0; i < numv; i++) {
      for(int j = 0; j < numv; j++) {
        assertEquals(mdist.distance(vs[i], vs[j]), dist.distance(vs[i], vs[j]), 1e-15);
      }
    }
  }
}
