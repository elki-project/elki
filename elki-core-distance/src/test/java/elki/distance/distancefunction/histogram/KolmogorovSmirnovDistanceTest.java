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

import org.junit.Test;

import elki.data.DoubleVector;
import elki.distance.distancefunction.AbstractDistanceTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for Kolmogorov-Smirnov based distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KolmogorovSmirnovDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    KolmogorovSmirnovDistance dist = new ELKIBuilder<>(KolmogorovSmirnovDistance.class).build();
    basicChecks(dist);
    double[] v1 = { .2, .2, .2, .2, .2 }; // uniform
    double[] v2 = { 0, 0, 1, 0, 0 }; // point
    double[] v3 = { 1, 0, 0, 0, 0 }; // point
    assertEquals(.4, dist.distance(DoubleVector.wrap(v1), DoubleVector.wrap(v2)), 0);
    assertEquals(.8, dist.distance(DoubleVector.wrap(v1), DoubleVector.wrap(v3)), 0);
    assertEquals(1, dist.distance(DoubleVector.wrap(v2), DoubleVector.wrap(v3)), 0);
  }
}
