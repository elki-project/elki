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
package elki.distance.minkowski;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.distance.AbstractDistanceTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for squared Euclidean distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SquaredEuclideanDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    SquaredEuclideanDistance dist = new ELKIBuilder<>(SquaredEuclideanDistance.class).build();
    basicChecks(dist);
    assertVaryingLengthBasic(dist, new double[] { 1, 0, 1, 1, 2, 1 }, 0);
    assertSpatialConsistency(dist);
    assertNonnegativeSpatialConsistency(dist);
    // Test low-level API:
    assertEquals("Basic 2", 1, dist.distance(BASIC[0].toArray(), BASIC[3].toArray()), 0);
  }
}
