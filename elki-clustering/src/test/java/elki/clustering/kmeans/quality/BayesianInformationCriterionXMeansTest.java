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
package elki.clustering.kmeans.quality;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.XMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for BIC in the Xmeans variant
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class BayesianInformationCriterionXMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testXMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<XMeans<DoubleVector, ?>>(XMeans.class) //
        .with(XMeans.Parameterizer.K_MIN_ID, 2) //
        .with(KMeans.K_ID, 20) //
        .with(XMeans.Parameterizer.INFORMATION_CRITERION_ID, BayesianInformationCriterionXMeans.class) //
        .with(KMeans.SEED_ID, 0) // // Initializer seed
        .with(XMeans.Parameterizer.SEED_ID, 0) // // X-means seed
        .build().run(db);
    testFMeasure(db, result, 0.960385);
    testClusterSizes(result, new int[] { 1, 2, 2, 2, 2, 2, 2, 3, 5, 50, 106, 153 });
  }
}
