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
package elki.clustering.em;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.em.models.BetulaClusterModelFactory;
import elki.clustering.em.models.BetulaDiagonalGaussianModelFactory;
import elki.clustering.em.models.BetulaMultivariateGaussianModelFactory;
import elki.clustering.em.models.BetulaSphericalGaussianModelFactory;
import elki.clustering.kmeans.initialization.betula.AbstractCFKMeansInitialization;
import elki.clustering.kmeans.initialization.betula.CFKPlusPlusLeaves;
import elki.data.Clustering;
import elki.database.Database;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CentroidEuclideanDistance;
import elki.index.tree.betula.distance.RadiusDistance;
import elki.index.tree.betula.distance.VarianceIncreaseDistance;
import elki.index.tree.betula.features.VIIFeature;
import elki.index.tree.betula.features.VVIFeature;
import elki.index.tree.betula.features.VVVFeature;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for BIRCH clustering with GMM.
 *
 * @author Andreas Lang
 * @since 0.8.0
 */
public class BetulaGMMTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSpherical() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaGMM.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VIIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(EM.Par.K_ID, 4) //
        .with(EM.Par.DELTA_ID, 1e-7)//
        .with(EM.Par.MODEL_ID, BetulaSphericalGaussianModelFactory.class)//
        .with(BetulaClusterModelFactory.INIT_ID, CFKPlusPlusLeaves.class)//
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.95991);
    assertClusterSizes(clustering, new int[] { 38, 189, 200, 211 });
  }

  @Test
  public void testDiagonal() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaGMM.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VVIFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, VarianceIncreaseDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(EM.Par.K_ID, 4) //
        .with(EM.Par.DELTA_ID, 1e-7)//
        .with(EM.Par.MODEL_ID, BetulaDiagonalGaussianModelFactory.class)//
        .with(BetulaClusterModelFactory.INIT_ID, CFKPlusPlusLeaves.class)//
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.8861344829);
    assertClusterSizes(clustering, new int[] { 118, 118, 200, 202 });
  }

  @Test
  public void testMultivariate() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BetulaGMM.class) //
        .with(CFTree.Factory.Par.FEATURES_ID, VVVFeature.Factory.class)//
        .with(CFTree.Factory.Par.ABSORPTION_ID, RadiusDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(EM.Par.K_ID, 4) //
        .with(EM.Par.DELTA_ID, 1e-7)//
        .with(EM.Par.MODEL_ID, BetulaMultivariateGaussianModelFactory.class)//
        .with(BetulaClusterModelFactory.INIT_ID, CFKPlusPlusLeaves.class)//
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.84929);
    assertClusterSizes(clustering, new int[] { 99, 101, 211, 227 });
  }
}
