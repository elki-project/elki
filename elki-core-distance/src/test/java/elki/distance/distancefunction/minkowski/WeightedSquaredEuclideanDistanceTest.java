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
package elki.distance.distancefunction.minkowski;

import java.util.Random;

import org.junit.Test;

import elki.distance.distancefunction.AbstractDistanceTest;
import elki.distance.distancefunction.WeightedNumberVectorDistance;
import elki.math.MathUtil;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for squared Euclidean distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class WeightedSquaredEuclideanDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder
    WeightedSquaredEuclideanDistance dist = new ELKIBuilder<>(WeightedSquaredEuclideanDistance.class) //
        .with(WeightedNumberVectorDistance.WEIGHTS_ID, MathUtil.randomDoubleArray(TEST_DIM, new Random(0L))) //
        .build();
    basicChecks(dist);
    spatialConsistency(dist);
    nonnegativeSpatialConsistency(dist);
  }
}
