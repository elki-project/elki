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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.kmeans.initialization.FirstK;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.KMeansModel;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.utilities.ELKIBuilder;

/**
 * Test cluster quality measure computations.
 *
 * @author Stephan Baier
 * @since 0.7.0
 */
public class WithinClusterVarianceTest extends AbstractClusterAlgorithmTest {
  /**
   * Test cluster variance.
   */
  @Test
  public void testVariance() {
    Database db = makeSimpleDatabase(UNITTEST + "quality-measure-test.csv", 7);
    Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

    // Setup algorithm
    LloydKMeans<DoubleVector> kmeans = new ELKIBuilder<LloydKMeans<DoubleVector>>(LloydKMeans.class) //
        .with(KMeans.K_ID, 2) //
        .with(KMeans.INIT_ID, FirstK.class) //
        .build();

    // run KMeans on database
    Clustering<KMeansModel> result = kmeans.run(db);

    // Test Cluster Variance
    KMeansQualityMeasure<? super DoubleVector> variance = new WithinClusterVariance();
    final NumberVectorDistance<? super DoubleVector> dist = kmeans.getDistance();

    final double quality = variance.quality(result, dist, rel);
    assertEquals("Within cluster variance incorrect", 3.16666666666, quality, 1e-10);
  }
}
