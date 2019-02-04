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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for cutting dendrograms at a given height.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CutDendrogramByNumberOfClustersTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSLINKResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 10) //
        .with(AbstractAlgorithm.ALGORITHM_ID, SLINK.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.9474250948);
    testClusterSizes(clustering, new int[] { 1, 1, 1, 1, 1, 2, 3, 62, 104, 154 });
  }

  @Test
  public void testSLINKHierarchical() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 10) //
        .with(CutDendrogramByNumberOfClusters.Parameterizer.HIERARCHICAL_ID) //
        .with(AbstractAlgorithm.ALGORITHM_ID, SLINK.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.9474250948);
    testClusterSizes(clustering, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 3, 62, 104, 154 });
  }
}
