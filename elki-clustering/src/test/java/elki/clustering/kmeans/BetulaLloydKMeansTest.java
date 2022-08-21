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
package elki.clustering.kmeans;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.initialization.betula.AbstractCFKMeansInitialization;
import elki.data.Clustering;
import elki.database.Database;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CentroidEuclideanDistance;
import elki.index.tree.betula.features.VIIFeature;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for BIRCH clustering with k-means.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class BetulaLloydKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testFull() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLloydKMeans.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.86656);
    assertClusterSizes(clustering, new int[] { 85, 127, 203, 223 });
  }

  @Test
  public void testStored() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLloydKMeans.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(BetulaLloydKMeans.Par.STORE_IDS_ID) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.86826);
    assertClusterSizes(clustering, new int[] { 87, 127, 203, 221 });
  }

  @Test
  public void testNaive() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLloydKMeans.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(BetulaLloydKMeans.Par.IGNORE_WEIGHT_ID) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.86656);
    assertClusterSizes(clustering, new int[] { 85, 127, 203, 223 });
  }

  @Test
  public void testNaiveStored() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLloydKMeans.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(BetulaLloydKMeans.Par.IGNORE_WEIGHT_ID) //
        .with(BetulaLloydKMeans.Par.STORE_IDS_ID) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.86826);
    assertClusterSizes(clustering, new int[] { 87, 127, 203, 221 });
  }
}
