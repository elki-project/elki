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
package elki.clustering.hierarchical;

import org.junit.Test;

import elki.AbstractAlgorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.utilities.ELKIBuilder;

/**
 * Perform HDBSCAN unit test
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HDBSCANLinearMemoryTest extends AbstractClusterAlgorithmTest {
  // TODO: add more data sets.

  /**
   * Run agglomerative hierarchical clustering with fixed parameters and compare
   * the result to a golden standard.
   */
  @Test
  public void testHDBSCAN() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 3) //
        .with(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20) //
        .build().run(db);
    testFMeasure(db, clustering, 0.686953412);
    testClusterSizes(clustering, new int[] { 1, 200, 437 });
  }

  /**
   * Regression test against github #46O
   */
  @Test
  public void testHDBSCANCornerCase() {
    Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(new double[][] { { 1, 0 }, { 0, 1 } }), null);
    db.initialize();
    new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 3) //
        .with(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20) //
        .build().run(db);
    db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(new double[][] { { 0 } }), null);
    db.initialize();
    new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 3) //
        .with(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20) //
        .build().run(db);
  }
}
