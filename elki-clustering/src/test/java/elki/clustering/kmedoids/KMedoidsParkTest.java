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
package elki.clustering.kmedoids;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmedoids.initialization.ParkJun;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.MedoidModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test k-Medoids using the Park approach.
 *
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class KMedoidsParkTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMedoidsPark() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<MedoidModel> result = new ELKIBuilder<KMedoidsPark<DoubleVector>>(KMedoidsPark.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.INIT_ID, ParkJun.class) //
        .build().run(db);
    testFMeasure(db, result, 0.998005);
    testClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }
}
