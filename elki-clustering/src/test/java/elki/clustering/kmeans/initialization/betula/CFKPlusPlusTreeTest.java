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
package elki.clustering.kmeans.initialization.betula;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.BetulaLloydKMeans;
import elki.data.Clustering;
import elki.database.Database;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CentroidEuclideanDistance;
import elki.index.tree.betula.features.VIIFeature;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for CF k-means++
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class CFKPlusPlusTreeTest extends AbstractClusterAlgorithmTest {
  @Test
  public void test() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLloydKMeans.class) //
        .with(BetulaLloydKMeans.INIT_ID, CFKPlusPlusTree.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.90380);
    assertClusterSizes(clustering, new int[] { 92, 142, 200, 204 });
  }

  @Test
  public void testFirstUniform() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaLloydKMeans.class) //
        .with(BetulaLloydKMeans.INIT_ID, CFKPlusPlusTree.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(CFKPlusPlusTree.Par.FIRST_UNIFORM_ID) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.889966);
    assertClusterSizes(clustering, new int[] { 107, 127, 200, 204 });
  }
}
