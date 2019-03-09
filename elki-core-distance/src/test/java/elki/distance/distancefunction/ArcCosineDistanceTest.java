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
package elki.distance.distancefunction;

import org.junit.Test;

import elki.math.MathUtil;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for ArcCosine distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ArcCosineDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    ArcCosineDistance dist = new ELKIBuilder<>(ArcCosineDistance.class).build();
    basicChecks(dist);
    // Note: some of these are not well defined, as we have zero vectors.
    varyingLengthBasic(0, dist, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI);
    nonnegativeSpatialConsistency(dist);
  }
}
