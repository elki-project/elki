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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for the Leader algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LeaderTest extends AbstractClusterAlgorithmTest {
  /**
   * Run Leader with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testLeaderResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<Leader<DoubleVector>>(Leader.class) //
        .with(Leader.Parameterizer.THRESHOLD_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.910848);
    testClusterSizes(result, new int[] { 1, 1, 1, 1, 1, 2, 3, 3, 3, 3, 13, 52, 105, 141 });
  }

  /**
   * Run Leader with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testLeaderOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> result = new ELKIBuilder<Leader<DoubleVector>>(Leader.class) //
        .with(Leader.Parameterizer.THRESHOLD_ID, 25) //
        .build().run(db);
    testFMeasure(db, result, 0.61614);
    testClusterSizes(result, new int[] { 4, 139, 147, 348 });
  }
}
