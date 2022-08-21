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
package elki.clustering;

import org.junit.Test;

import elki.data.Clustering;
import elki.database.Database;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.AverageInterclusterDistance;
import elki.index.tree.betula.distance.AverageIntraclusterDistance;
import elki.index.tree.betula.distance.CentroidEuclideanDistance;
import elki.index.tree.betula.distance.CentroidManhattanDistance;
import elki.index.tree.betula.features.VIIFeature;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for Betula clustering.
 *
 * @author Andreas Lang
 * @since 0.8.0
 */
public class BetulaLeafPreClusteringTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testRadius() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.8619);
    assertClusterSizes(clustering, new int[] { 105, 112, 200, 221 });
  }

  @Test
  public void testEuclideanDistance() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testCentroidEuclidean() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, AverageInterclusterDistance.class)//
        .with(CFTree.Factory.Par.DISTANCE_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93909);
    assertClusterSizes(clustering, new int[] { 72, 168, 198, 200 });
  }

  @Test
  public void testCentroidManhattan() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, AverageIntraclusterDistance.class)//
        .with(CFTree.Factory.Par.DISTANCE_ID, CentroidManhattanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.92236);
    assertClusterSizes(clustering, new int[] { 83, 154, 200, 201 });
  }

  @Test
  public void testAverageIntercluster() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, AverageIntraclusterDistance.class)//
        .with(CFTree.Factory.Par.DISTANCE_ID, AverageInterclusterDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.860623);
    assertClusterSizes(clustering, new int[] { 102, 114, 200, 222 });
  }

  @Test
  public void testAverageIntracluster() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, AverageIntraclusterDistance.class)//
        .with(CFTree.Factory.Par.DISTANCE_ID, AverageIntraclusterDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.820235);
    assertClusterSizes(clustering, new int[] { 158, 224, 256 });
  }

  @Test
  public void testOverflowing() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLeafPreClustering.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, AverageIntraclusterDistance.class)//
        .with(CFTree.Factory.Par.BRANCHING_ID, 4) // Force branching
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.8857);
    assertClusterSizes(clustering, new int[] { 71, 147, 199, 221 });
  }
}
