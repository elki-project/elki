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
package elki.clustering.kmedoids;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.MedoidModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for CLARANS+
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FastCLARANSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testFastCLARANS() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<MedoidModel> result = new ELKIBuilder<FastCLARANS<DoubleVector>>(FastCLARANS.class) //
        .with(KMeans.K_ID, 5) //
        .with(CLARANS.Par.RANDOM_ID, 16) //
        .with(CLARANS.Par.NEIGHBORS_ID, 3) //
        .with(CLARANS.Par.RESTARTS_ID, 5) //
        .build().autorun(db);
    // FastCLARANS finds better solution than CLARANS in this unit test.
    // We actually had to vary the random seed to not get 1.0
    assertFMeasure(db, result, .99602);
    assertClusterSizes(result, new int[] { 198, 200, 200, 200, 202 });
  }

  @Test
  public void testFastCLARANSNoise() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<FastCLARANS<DoubleVector>>(FastCLARANS.class) //
        .with(KMeans.K_ID, 3) //
        .with(CLARANS.Par.RANDOM_ID, 0) //
        .with(CLARANS.Par.NEIGHBORS_ID, .1) //
        .with(CLARANS.Par.RESTARTS_ID, 5) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.913858);
    assertClusterSizes(result, new int[] { 57, 115, 158 });
  }
}
