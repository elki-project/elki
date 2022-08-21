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
package elki.clustering.hierarchical.extraction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.HDBSCANLinearMemory;
import elki.clustering.hierarchical.MiniMaxNNChain;
import elki.clustering.hierarchical.NNChain;
import elki.clustering.hierarchical.SLINK;
import elki.clustering.hierarchical.linkage.SingleLinkage;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

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
        .with(SimplifiedHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.696491);
    assertClusterSizes(clustering, new int[] { 3, 5, 43, 55, 58, 62, 104 });
  }

  @Test
  public void testNNChainResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(NNChain.Par.LINKAGE_ID, SingleLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.696491);
    assertClusterSizes(clustering, new int[] { 3, 5, 43, 55, 58, 62, 104 });
  }

  @Test
  public void testSLINKDegenerate() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 1) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.0182169); // minclustersize=1 is useless
    assertEquals(2 * 330 - 1, clustering.getAllClusters().size());
  }

  @Test
  public void testHDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<DendrogramModel> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Par.MIN_PTS_ID, 20) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.96782);
    assertClusterSizes(clustering, new int[] { 7, 13, 55, 103, 152 });
  }

  @Test
  public void testHDBSCANDegenerate() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<DendrogramModel> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 1) //
        .with(Algorithm.Utils.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Par.MIN_PTS_ID, 20) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.0182169); // minclustersize=1 is useless
    // FIXME: investigate this: assertEquals(2 * 330 - 1,
    // clustering.getAllClusters().size());
  }

  @Test
  public void testMiniMaxNNResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<DendrogramModel> clustering = new ELKIBuilder<>(SimplifiedHierarchyExtraction.class) //
        .with(SimplifiedHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 1) //
        .with(Algorithm.Utils.ALGORITHM_ID, MiniMaxNNChain.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.0182169); // minclustersize=1 is useless
    assertEquals(2 * 330 - 1, clustering.getAllClusters().size());
  }
}
