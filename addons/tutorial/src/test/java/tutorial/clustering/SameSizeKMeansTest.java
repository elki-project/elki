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
package tutorial.clustering;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test the same-size k-means algorithm
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class SameSizeKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<>(SameSizeKMeans.class) //
        .with(KMeans.K_ID, 6) //
        .with(KMeans.SEED_ID, 2) //
        .build().autorun(db);
    // With k=5, it achieves even 100% on this data set, but it is more useful
    // for regression testing to use suboptimal results.
    assertFMeasure(db, result, 0.752495);
    assertClusterSizes(result, new int[] { 166, 166, 167, 167, 167, 167 });
  }
}
