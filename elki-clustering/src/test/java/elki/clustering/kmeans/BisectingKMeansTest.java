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
import elki.clustering.kmeans.quality.WithinClusterVariance;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.MeanModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Tests the BisectingKMeans
 * 
 * @author Stephan Baier
 * @since 0.7.0
 */
public class BisectingKMeansTest extends AbstractClusterAlgorithmTest {
  /**
   * Run BisectingKMeans with fixed parameters and compare cluster size to
   * expected value.
   */
  @Test
  public void testKMeansBisectingClusterSize() {
    Database db = makeSimpleDatabase(UNITTEST + "bisecting-test.csv", 300);
    Clustering<MeanModel> result = new ELKIBuilder<BisectingKMeans<DoubleVector, MeanModel>>(BisectingKMeans.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 0) //
        .with(BestOfMultipleKMeans.Parameterizer.TRIALS_ID, 5) //
        .with(BestOfMultipleKMeans.Parameterizer.KMEANS_ID, LloydKMeans.class) //
        .with(BestOfMultipleKMeans.Parameterizer.QUALITYMEASURE_ID, WithinClusterVariance.class) //
        .build().run(db);
    testClusterSizes(result, new int[] { 103, 97, 100 });
  }

  /**
   * Run BisectingKMeans with fixed parameters (k = 2) and compare f-measure to
   * golden standard.
   */
  @Test
  public void testKMeansBisectingFMeasure() {
    Database db = makeSimpleDatabase(UNITTEST + "bisecting-test.csv", 300);

    BisectingKMeans<DoubleVector, MeanModel> kmeans = new ELKIBuilder<BisectingKMeans<DoubleVector, MeanModel>>(BisectingKMeans.class) //
        .with(KMeans.K_ID, 2) //
        .with(KMeans.SEED_ID, 0) //
        .with(BestOfMultipleKMeans.Parameterizer.TRIALS_ID, 5) //
        .with(BestOfMultipleKMeans.Parameterizer.KMEANS_ID, LloydKMeans.class) //
        .with(BestOfMultipleKMeans.Parameterizer.QUALITYMEASURE_ID, WithinClusterVariance.class) //
        .build();

    // run KMedians on database
    Clustering<MeanModel> result = kmeans.run(db);
    testFMeasure(db, result, 0.7408);
  }
}
