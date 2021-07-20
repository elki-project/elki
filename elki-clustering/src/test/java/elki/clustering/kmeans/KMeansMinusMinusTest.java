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
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test the k-means-- algorithm
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KMeansMinusMinusTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeansMinusMinusRateZero() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KMeansMinusMinus<DoubleVector>>(KMeansMinusMinus.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansMinusMinus.Par.RATE_ID, 0.) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.9980050099749501);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testKMeansMinusMinusNoiseFlag() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KMeansMinusMinus<DoubleVector>>(KMeansMinusMinus.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 0) //
        .with(KMeansMinusMinus.Par.RATE_ID, 0.1) //
        .with(KMeansMinusMinus.Par.NOISE_FLAG_ID, true) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.9303073926666102);
    assertClusterSizes(result, new int[] { 100, 112, 188, 200, 200, 200 });
  }

}
