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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.FirstKInitialMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test cluster quality measure computations.
 *
 * @author Stephan Baier
 * @since 0.7.0
 */
public class WithinClusterMeanDistanceQualityMeasureTest extends AbstractClusterAlgorithmTest {
  /**
   * Test cluster average overall distance.
   */
  @Test
  public void testOverallDistance() {
    Database db = makeSimpleDatabase(UNITTEST + "quality-measure-test.csv", 7);
    Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

    KMeansLloyd<DoubleVector> kmeans = new ELKIBuilder<KMeansLloyd<DoubleVector>>(KMeansLloyd.class) //
        .with(KMeans.K_ID, 2) //
        .with(KMeans.INIT_ID, FirstKInitialMeans.class) //
        .build();

    // run KMeans on database
    Clustering<KMeansModel> result = kmeans.run(db);
    final NumberVectorDistanceFunction<? super DoubleVector> dist = kmeans.getDistanceFunction();

    // Test Cluster Average Overall Distance
    KMeansQualityMeasure<? super DoubleVector> overall = new WithinClusterMeanDistanceQualityMeasure();
    final double quality = overall.quality(result, dist, rel);

    assertEquals("Avarage overall distance not as expected.", 0.8888888888888888, quality, 1e-10);
  }
}
