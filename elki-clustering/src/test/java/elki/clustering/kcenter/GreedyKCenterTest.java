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
package elki.clustering.kcenter;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.distance.minkowski.ManhattanDistance;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for {@link GreedyKCenter} clustering.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class GreedyKCenterTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testEuclidean() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<GreedyKCenter<DoubleVector>>(GreedyKCenter.class) //
        .with(GreedyKCenter.Par.K_ID, 3) //
        .with(GreedyKCenter.Par.RANDOM_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.91352);
    assertClusterSizes(result, new int[] { 55, 118, 157 });
  }

  @Test
  public void testManhattan() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<GreedyKCenter<DoubleVector>>(GreedyKCenter.class) //
        .with(Algorithm.Utils.DISTANCE_FUNCTION_ID, ManhattanDistance.STATIC) //
        .with(GreedyKCenter.Par.K_ID, 3) //
        .with(GreedyKCenter.Par.RANDOM_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.91436);
    assertClusterSizes(result, new int[] { 56, 117, 157 });
  }
}
