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
import elki.clustering.hierarchical.linkage.*;
import elki.data.Clustering;
import elki.database.Database;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.utilities.ELKIBuilder;

/**
 * Perform agglomerative hierarchical clustering, using the NN chain algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class NNChainTest extends AbstractClusterAlgorithmTest {
  // TODO: add more data sets.
  @Test
  public void testSingleLink() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, SingleLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.6829722);
    assertClusterSizes(clustering, new int[] { 9, 200, 429 });
  }

  @Test
  public void testWard() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, WardLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testGroupAverage() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, GroupAverageLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testWeightedAverage() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, WeightedAverageLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testCompleteLink() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, CompleteLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.938167802);
    assertClusterSizes(clustering, new int[] { 200, 217, 221 });
  }

  @Test
  public void testCentroid() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, CentroidLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testMedian() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, MedianLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9386626);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  // Note: squared Euclidean result differs, matches LinearMemoryNNChain:
  @Test
  public void testMedianSquared() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(Algorithm.Utils.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.class)//
        .with(AGNES.Par.LINKAGE_ID, MedianLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9381678);
    assertClusterSizes(clustering, new int[] { 200, 217, 221 });
  }

  @Test
  public void testMinimumVariance() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, MinimumVarianceLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testBetaVariance() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(AGNES.Par.LINKAGE_ID, FlexibleBetaLinkage.class) //
        .with(FlexibleBetaLinkage.Par.BETA_ID, -.33) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9381678);
    assertClusterSizes(clustering, new int[] { 200, 217, 221 });
  }
}
