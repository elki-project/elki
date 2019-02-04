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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality.WithinClusterVarianceQualityMeasure;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the KMeansBisecting
 * 
 * @author Stephan Baier
 * @since 0.7.0
 */
public class KMeansBisectingTest extends AbstractClusterAlgorithmTest {
  /**
   * Run KMeansBisecting with fixed parameters and compare cluster size to
   * expected value.
   */
  @Test
  public void testKMeansBisectingClusterSize() {
    Database db = makeSimpleDatabase(UNITTEST + "bisecting-test.csv", 300);
    Clustering<MeanModel> result = new ELKIBuilder<KMeansBisecting<DoubleVector, MeanModel>>(KMeansBisecting.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 0) //
        .with(BestOfMultipleKMeans.Parameterizer.TRIALS_ID, 5) //
        .with(BestOfMultipleKMeans.Parameterizer.KMEANS_ID, KMeansLloyd.class) //
        .with(BestOfMultipleKMeans.Parameterizer.QUALITYMEASURE_ID, WithinClusterVarianceQualityMeasure.class) //
        .build().run(db);
    testClusterSizes(result, new int[] { 103, 97, 100 });
  }

  /**
   * Run KMeansBisecting with fixed parameters (k = 2) and compare f-measure to
   * golden standard.
   */
  @Test
  public void testKMeansBisectingFMeasure() {
    Database db = makeSimpleDatabase(UNITTEST + "bisecting-test.csv", 300);

    KMeansBisecting<DoubleVector, MeanModel> kmeans = new ELKIBuilder<KMeansBisecting<DoubleVector, MeanModel>>(KMeansBisecting.class) //
        .with(KMeans.K_ID, 2) //
        .with(KMeans.SEED_ID, 0) //
        .with(BestOfMultipleKMeans.Parameterizer.TRIALS_ID, 5) //
        .with(BestOfMultipleKMeans.Parameterizer.KMEANS_ID, KMeansLloyd.class) //
        .with(BestOfMultipleKMeans.Parameterizer.QUALITYMEASURE_ID, WithinClusterVarianceQualityMeasure.class) //
        .build();

    // run KMedians on database
    Clustering<MeanModel> result = kmeans.run(db);
    testFMeasure(db, result, 0.7408);
  }
}
