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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elki.distance.AbstractDistanceTest;
import elki.utilities.ELKIBuilder;
import net.jafama.FastMath;

/**
 * Unit test for Minkowski L<sub>p</sub> distances.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LPNormDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    LPNormDistance dist = new ELKIBuilder<>(LPNormDistance.class) //
        .with(LPNormDistance.Par.P_ID, .5) //
        .build();
    assertSame("Subtyped", LPNormDistance.class, dist.getClass());
    assertFalse("Not metric", dist.isMetric());
    basicChecks(dist);
    varyingLengthBasic(0, dist, 1, 0, 1, 1, 4, 1);
    spatialConsistency(dist);
    nonnegativeSpatialConsistency(dist);
    dist = new ELKIBuilder<>(LPNormDistance.class) //
        .with(LPNormDistance.Par.P_ID, 3) //
        .build();
    assertSame("Not optimized", LPIntegerNormDistance.class, dist.getClass());
    assertTrue("Not metric", dist.isMetric());
    basicChecks(dist);
    varyingLengthBasic(0, dist, 1, 0, 1, 1, FastMath.pow(2, 1. / 3), 1);
    spatialConsistency(dist);
    nonnegativeSpatialConsistency(dist);
  }
}
