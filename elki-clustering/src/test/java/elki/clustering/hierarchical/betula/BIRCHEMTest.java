/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2021
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
package elki.clustering.hierarchical.betula;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.betula.vii.CentroidEuclideanDistance;
import elki.clustering.hierarchical.betula.vvv.RadiusDistance;
import elki.clustering.hierarchical.betula.vii.VIIModel;
import elki.clustering.hierarchical.betula.vvi.VarianceIncreaseDistance;
import elki.clustering.hierarchical.betula.vvv.VVVModel;
import elki.clustering.hierarchical.betula.vvi.VVIModel;
import elki.clustering.kmeans.AbstractKMeans;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;
import elki.clustering.hierarchical.betula.initialization.AbstractCFKMeansInitialization;
import elki.clustering.hierarchical.betula.initialization.CFKMeansPlusPlus;

/**
 * Regression test for BIRCH clustering with GMM.
 *
 * @author Andreas Lang
 */
public class BIRCHEMTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSpherical() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHEM.class) //
        .with(CFTree.Factory.Par.MODEL_ID, VIIModel.class)//
        .with(VIIModel.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(BIRCHEM.Par.DELTA_ID, 1e-7)//
        .with(BIRCHEM.Par.INIT_ID, EMSphericalInitializer.class)//
        .with(AbstractEMInitializer.Par.INIT_ID, CFKMeansPlusPlus.class)//
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.865976561);
    assertClusterSizes(clustering, new int[] { 100, 117, 200, 221 });
  }

  @Test
  public void testDiagonal() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHEM.class) //
        .with(CFTree.Factory.Par.MODEL_ID, VVIModel.class)//
        .with(VVIModel.Par.ABSORPTION_ID, VarianceIncreaseDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(BIRCHEM.Par.DELTA_ID, 1e-7)//
        .with(BIRCHEM.Par.INIT_ID, EMDiagonalInitializer.class)//
        .with(AbstractEMInitializer.Par.INIT_ID, CFKMeansPlusPlus.class)//
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.8416016383);
    assertClusterSizes(clustering, new int[] { 124, 142, 172, 200 });
  }

  @Test
  public void testMultivariate() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHEM.class) //
        .with(CFTree.Factory.Par.MODEL_ID, VVVModel.class)//
        .with(VVVModel.Par.ABSORPTION_ID, RadiusDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(BIRCHEM.Par.DELTA_ID, 1e-7)//
        .with(BIRCHEM.Par.INIT_ID, EMMultivariateInitializer.class)//
        .with(AbstractEMInitializer.Par.INIT_ID, CFKMeansPlusPlus.class)//
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.84929430390);
    assertClusterSizes(clustering, new int[] { 99, 101, 211, 227 });
  }

}
