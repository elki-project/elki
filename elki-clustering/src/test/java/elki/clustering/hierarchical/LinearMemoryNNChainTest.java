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
package elki.clustering.hierarchical;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.clustering.hierarchical.linkage.CentroidLinkage;
import elki.clustering.hierarchical.linkage.GroupAverageLinkage;
import elki.clustering.hierarchical.linkage.MedianLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test the linear-memory version of NNChain
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class LinearMemoryNNChainTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testWard() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, LinearMemoryNNChain.class) //
        .with(LinearMemoryNNChain.Par.LINKAGE_ID, WardLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  // Note: squared Euclidean, not regular Euclidean; but same result here
  @Test
  public void testGroupAverage() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, LinearMemoryNNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, GroupAverageLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }
  
  @Test
  public void testCentroid() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, LinearMemoryNNChain.class) //
        .with(LinearMemoryNNChain.Par.LINKAGE_ID, CentroidLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  // Note: squared Euclidean, not regular Euclidean for geometric Linkage
  @Test
  public void testSquaredMedian() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, LinearMemoryNNChain.class) //
        .with(LinearMemoryNNChain.Par.LINKAGE_ID, MedianLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9381678);
    assertClusterSizes(clustering, new int[] { 200, 217, 221 });
  }
}
