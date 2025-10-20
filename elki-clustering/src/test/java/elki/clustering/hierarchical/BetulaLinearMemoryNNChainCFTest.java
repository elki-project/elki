/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2024
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
import elki.data.Clustering;
import elki.database.Database;
import static elki.index.tree.betula.CFTree.Factory.Par.MAXLEAVES_ID;
import static elki.index.tree.betula.CFTree.Factory.Par.DISTANCE_ID;

import elki.index.tree.betula.distance.AverageIntraclusterDistance;
import elki.index.tree.betula.distance.CentroidEuclideanDistance;
import elki.index.tree.betula.distance.VarianceIncreaseDistance;
import elki.utilities.ELKIBuilder;

/**
 * Test Betula Linear Memor NNChain CF
 *
 * @author Andreas Lang
 */
public class BetulaLinearMemoryNNChainCFTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testWard() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, BetulaLinearMemoryNNChainCF.class) //
        .with(DISTANCE_ID, VarianceIncreaseDistance.class) //
        .with(MAXLEAVES_ID, 800) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testCentroid() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, BetulaLinearMemoryNNChainCF.class) //
        .with(DISTANCE_ID, CentroidEuclideanDistance.class) //
        .with(MAXLEAVES_ID, 800) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testUPGMA() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, BetulaLinearMemoryNNChainCF.class) //
        .with(DISTANCE_ID, AverageIntraclusterDistance.class) //
        .with(MAXLEAVES_ID, 800) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testWardA() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, BetulaLinearMemoryNNChainCF.class) //
        .with(DISTANCE_ID, VarianceIncreaseDistance.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9402430606721007);
    assertClusterSizes(clustering, new int[] { 200, 203, 235 });
  }

  @Test
  public void testCentroidA() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, BetulaLinearMemoryNNChainCF.class) //
        .with(DISTANCE_ID, CentroidEuclideanDistance.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866264);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testUPGMAA() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, BetulaLinearMemoryNNChainCF.class) //
        .with(DISTANCE_ID, AverageIntraclusterDistance.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866264);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

}
