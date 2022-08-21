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
import elki.clustering.kmeans.KDTreePruningKMeans.Split;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for Pruning k-means.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class KDTreePruningKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKDTreePruningKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KDTreePruningKMeans<DoubleVector>>(KDTreePruningKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testKDTreePruningMedianKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KDTreePruningKMeans<DoubleVector>>(KDTreePruningKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .with(KDTreePruningKMeans.Par.SPLIT_ID, Split.MEDIAN) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testKDTreePruningBoundedKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KDTreePruningKMeans<DoubleVector>>(KDTreePruningKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .with(KDTreePruningKMeans.Par.SPLIT_ID, Split.BOUNDED_MIDPOINT) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testKDTreePruningSSQKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<KDTreePruningKMeans<DoubleVector>>(KDTreePruningKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .with(KDTreePruningKMeans.Par.SPLIT_ID, Split.SSQ) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }
}
