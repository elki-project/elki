/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2020
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
import elki.clustering.hierarchical.betula.vii.VIIModel;
import elki.clustering.kmeans.AbstractKMeans;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;
import elki.clustering.hierarchical.betula.initialization.AbstractCFKMeansInitialization;

/**
 * Regression test for BIRCH clustering with k-means.
 *
 * @author Erich Schubert
 */
public class BIRCHLloydKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testEuclideanDistance() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLloydKMeans.class) //
        .with(CFTree.Factory.Par.MODEL_ID, VIIModel.class)//
        .with(VIIModel.Par.ABSORPTION_ID, CentroidEuclideanDistance.class)//
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 50) //
        .with(AbstractKMeans.K_ID, 4) //
        .with(AbstractCFKMeansInitialization.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.84932);
    assertClusterSizes(clustering, new int[] { 98, 102, 211, 227 });
  }
}
