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
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full SNNClustering run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that SNNClustering's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SNNClusteringTest extends AbstractClusterAlgorithmTest {
  /**
   * Run SNNClustering with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testSNNClusteringResults() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d.ascii", 1200);
    Clustering<Model> result = new ELKIBuilder<SNNClustering<DoubleVector>>(SNNClustering.class) //
        .with(SNNClustering.Parameterizer.EPSILON_ID, 77) //
        .with(SNNClustering.Parameterizer.MINPTS_ID, 28) //
        .with(SharedNearestNeighborPreprocessor.Factory.NUMBER_OF_NEIGHBORS_ID, 100) //
        .build().run(db);
    testFMeasure(db, result, 0.832371422);
    testClusterSizes(result, new int[] { 73, 228, 213, 219, 231, 236 });
  }
}
