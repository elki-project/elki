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

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for Canberra distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class WeightedCanberraDistanceFunctionTest extends AbstractDistanceFunctionTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder
    WeightedCanberraDistanceFunction dist = new ELKIBuilder<>(WeightedCanberraDistanceFunction.class) //
        .with(WeightedNumberVectorDistanceFunction.WEIGHTS_ID, MathUtil.randomDoubleArray(TEST_DIM, new Random(0L))) //
        .build();
    basicChecks(dist);
    spatialConsistency(dist);
    nonnegativeSpatialConsistency(dist);
  }
}
