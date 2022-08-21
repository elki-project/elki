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

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.NNChain;
import elki.clustering.hierarchical.SLINK;
import elki.clustering.hierarchical.linkage.SingleLinkage;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for cutting dendrograms at a given height.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CutDendrogramByHeightTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSLINKResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByHeight.class) //
        .with(CutDendrogramByHeight.Par.THRESHOLD_ID, 0.14) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9474250948);
    assertClusterSizes(clustering, new int[] { 1, 1, 1, 1, 1, 2, 3, 62, 104, 154 });
  }

  @Test
  public void testSLINKHiearchical() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByHeight.class) //
        .with(CutDendrogramByHeight.Par.THRESHOLD_ID, 0.14) //
        .with(CutDendrogramByHeight.Par.HIERARCHICAL_ID) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9474250948);
    // With simplification, fewer 0-clusters:
    assertClusterSizes(clustering, new int[] { 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 3, 62, 104, 154 });
  }

  @Test
  public void testSLINKNoSimplify() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByHeight.class) //
        .with(CutDendrogramByHeight.Par.THRESHOLD_ID, 0.14) //
        .with(CutDendrogramByHeight.Par.HIERARCHICAL_ID) //
        .with(CutDendrogramByHeight.Par.NOSIMPLIFY_ID) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9474250948);
    // Without simplification, 5 additional 0-clusters:
    assertClusterSizes(clustering, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 3, 62, 104, 154 });
  }

  @Test
  public void testNNChainHiearchical() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByHeight.class) //
        .with(CutDendrogramByHeight.Par.THRESHOLD_ID, 0.14) //
        .with(CutDendrogramByHeight.Par.HIERARCHICAL_ID) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(NNChain.Par.LINKAGE_ID, SingleLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9474250948);
    assertClusterSizes(clustering, new int[] { 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 3, 62, 104, 154 });
    // With simplification, fewer 0-clusters:
  }
}
