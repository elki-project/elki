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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Perform agglomerative hierarchical clustering, using the naive algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MiniMaxAnderbergTest extends AbstractClusterAlgorithmTest {
  // TODO: add more data sets.

  /**
   * Run agglomerative hierarchical clustering with fixed parameters and compare
   * the result to a golden standard.
   */
  @Test
  public void testMiniMax() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 3) //
        .with(AbstractAlgorithm.ALGORITHM_ID, MiniMaxAnderberg.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.938662648);
    testClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  /**
   * Run agglomerative hierarchical clustering with fixed parameters and compare
   * the result to a golden standard.
   */
  @Test
  public void testMiniMax2() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 3) //
        .with(AbstractAlgorithm.ALGORITHM_ID, MiniMaxAnderberg.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.914592130);
    testClusterSizes(clustering, new int[] { 59, 112, 159 });
  }
}
