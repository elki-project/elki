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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.XMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for BIC in the Zhao variant
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class BayesianInformationCriterionZhaoTest extends AbstractClusterAlgorithmTest {
  /**
   * Test with x-means
   */
  @Test
  public void testXMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<XMeans<DoubleVector, ?>>(XMeans.class) //
        .with(XMeans.Parameterizer.K_MIN_ID, 2) //
        .with(KMeans.K_ID, 20) //
        .with(XMeans.Parameterizer.INNER_KMEANS_ID, KMeansLloyd.class) //
        .with(XMeans.Parameterizer.INFORMATION_CRITERION_ID, BayesianInformationCriterionZhao.class) //
        .with(KMeans.SEED_ID, 0) // // Initializer seed
        .with(XMeans.Parameterizer.SEED_ID, 0) // // X-means seed
        .build().run(db);
    testFMeasure(db, result, 0.9691);
    testClusterSizes(result, new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 50, 104, 152 });
  }
}
