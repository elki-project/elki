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
package elki.clustering.kmedoids.initialization;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmedoids.CLARA;
import elki.clustering.kmedoids.PAM;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Unit test the GreedyG initialization.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class GreedyGTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testPAMGreedyG() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<PAM<DoubleVector>>(PAM.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.INIT_ID, GreedyG.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.99800500);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testCLARAGreedyG() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<CLARA<DoubleVector>>(CLARA.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.INIT_ID, GreedyG.class) //
        .with(KMeans.MAXITER_ID, 1) //
        .with(CLARA.Par.NOKEEPMED_ID) //
        .with(CLARA.Par.SAMPLESIZE_ID, 10) //
        .with(CLARA.Par.RANDOM_ID, 1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.996);
    assertClusterSizes(result, new int[] { 198, 200, 200, 200, 202 });
  }
}
