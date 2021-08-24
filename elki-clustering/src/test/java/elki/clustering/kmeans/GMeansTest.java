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
package elki.clustering.kmeans;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for G-Means.
 * 
 * @author Robert Gehde
 */
public class GMeansTest extends AbstractClusterAlgorithmTest {
  /**
   * G-means test run.
   */
  @Test
  public void testGMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<GMeans<DoubleVector, ?>>(GMeans.class) //
        .with(GMeans.Par.K_MIN_ID, 2) //
        .with(KMeans.K_ID, 20) //
        .with(GMeans.Par.INNER_KMEANS_ID, ExponionKMeans.class) //
        .with(KMeans.SEED_ID, 0) // // Initializer seed
        .with(GMeans.Par.SEED_ID, 0) // // X-means seed
        .with(GMeans.Par.ALPHA_ID, 0.0001) // // Significance level
        .build().autorun(db);
    assertFMeasure(db, result, 0.5941747572815534);
    assertClusterSizes(result, new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 4, 5, 41, 46, 51, 51, 53, 61 });
  }
}
