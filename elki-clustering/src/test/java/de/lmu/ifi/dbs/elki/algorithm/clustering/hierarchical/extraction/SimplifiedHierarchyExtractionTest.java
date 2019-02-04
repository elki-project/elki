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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.MiniMaxNNChain;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for simplified hierarchy extraction.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SimplifiedHierarchyExtractionTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSLINKResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 50) //
        .with(AbstractAlgorithm.ALGORITHM_ID, SLINK.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.696491);
    testClusterSizes(clustering, new int[] { 3, 5, 43, 55, 58, 62, 104 });
  }

  @Test
  public void testSLINKDegenerate() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 1) //
        .with(AbstractAlgorithm.ALGORITHM_ID, SLINK.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.0182169); // minclustersize=1 is useless
    assertEquals(2 * 330 - 1, clustering.getAllClusters().size());
  }

  @Test
  public void testHDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Result result = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 50) //
        .with(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20) //
        .build().run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.96941);
    testClusterSizes(clustering, new int[] { 7, 14, 54, 103, 152 });
  }

  @Test
  public void testHDBSCANDegenerate() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Result result = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 1) //
        .with(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20) //
        .build().run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.0182169); // minclustersize=1 is useless
    assertEquals(2 * 330 - 1, clustering.getAllClusters().size());
  }

  @Test
  public void testMiniMaxNNResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Result result = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 1) //
        .with(AbstractAlgorithm.ALGORITHM_ID, MiniMaxNNChain.class) //
        .build().run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.0182169); // minclustersize=1 is useless
    assertEquals(2 * 330 - 1, clustering.getAllClusters().size());
  }
}
