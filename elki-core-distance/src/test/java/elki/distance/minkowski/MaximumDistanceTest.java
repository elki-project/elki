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
package elki.distance.minkowski;

import org.junit.Test;

import elki.distance.AbstractDistanceTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for minimum distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MaximumDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    MaximumDistance dist = new ELKIBuilder<>(MaximumDistance.class).build();
    basicChecks(dist);
    varyingLengthBasic(0, dist, 1, 0, 1, 1, 1, 1);
    spatialConsistency(dist);
    nonnegativeSpatialConsistency(dist);
  }
}
