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
package elki.clustering.kmedoids.initialization;

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
 * Test the O(kn) initialization "linear BUILD" from PAM++.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LABTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testLinearBUILDInitialMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<SingleAssignmentKMeans<DoubleVector>>(SingleAssignmentKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeans.INIT_ID, LAB.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.98818);
    assertClusterSizes(result, new int[] { 194, 200, 200, 200, 206 });
  }

  @Test
  public void testLinearBUILDInitialMedoids() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<CLARA<DoubleVector>>(CLARA.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 5) //
        .with(KMeans.INIT_ID, LAB.class) //
        .with(KMeans.MAXITER_ID, 1) //
        .with(CLARA.Par.NOKEEPMED_ID) //
        .with(CLARA.Par.SAMPLESIZE_ID, 10) //
        .with(CLARA.Par.RANDOM_ID, 2) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.9730);
    assertClusterSizes(result, new int[] { 186, 200, 200, 200, 214 });
  }
}
