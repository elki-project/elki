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
package elki.clustering.kmeans.spherical;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.distance.CosineDistance;
import elki.distance.SqrtCosineDistance;
import elki.utilities.ELKIBuilder;

/**
 * Regression test spherical k-means (although on not very well suited data).
 *
 * @author Erich Schubert
 */
public class SphericalKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSphericalKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<SphericalKMeans<DoubleVector>>(SphericalKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8625509);
    assertClusterSizes(result, new int[] { 131, 200, 200, 203, 266 });
    // Results should be similar to:
    result = new ELKIBuilder<LloydKMeans<DoubleVector>>(LloydKMeans.class) //
        .with(KMeans.DISTANCE_FUNCTION_ID, SqrtCosineDistance.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.864004);
    assertClusterSizes(result, new int[] { 132, 200, 200, 202, 266 });
    // Results should be similar to:
    result = new ELKIBuilder<LloydKMeans<DoubleVector>>(LloydKMeans.class) //
        .with(KMeans.DISTANCE_FUNCTION_ID, CosineDistance.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.864004);
    assertClusterSizes(result, new int[] { 132, 200, 200, 202, 266 });
  }
}
