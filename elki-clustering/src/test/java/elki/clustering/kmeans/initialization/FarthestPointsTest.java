/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package elki.clustering.kmeans.initialization;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.SingleAssignmentKMeans;
import elki.clustering.kmedoids.CLARA;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a single assignment with different k-means initializations.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FarthestPointsTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testFarthestPointsInitialMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<SingleAssignmentKMeans<DoubleVector>>(SingleAssignmentKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 3) //
        .with(KMeans.INIT_ID, FarthestPoints.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.88132453);
    assertClusterSizes(result, new int[] { 128, 199, 201, 201, 271 });
  }

  @Test
  public void testFarthestPointsInitialMedoids() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<CLARA<DoubleVector>>(CLARA.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 3) //
        .with(KMeans.INIT_ID, FarthestPoints.class) //
        .with(KMeans.MAXITER_ID, 1) //
        .with(CLARA.Par.SAMPLESIZE_ID, 10) //
        .with(CLARA.Par.RANDOM_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.98818);
    assertClusterSizes(result, new int[] { 194, 200, 200, 200, 206 });
  }
}
