/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the k-means-- algorithm
 *
 * @author Erich Schubert
 * @since 0.7.2
 */
public class KMeansMinusMinusTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeansMinusMinusRateZero() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.K_ID, 5);
    params.addParameter(KMeans.SEED_ID, 7);
    params.addParameter(KMeansMinusMinus.Parameterizer.RATE_ID, 0.);
    AbstractKMeans<DoubleVector, ?> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeansMinusMinus.class, params);
    testParameterizationOk(params);

    // run KMeans on database
    Clustering<?> result = kmeans.run(db);
    testFMeasure(db, result, 0.998005);
    testClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testKMeansMinusMinus() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.K_ID, 5);
    params.addParameter(KMeans.SEED_ID, 7);
    params.addParameter(KMeansMinusMinus.Parameterizer.RATE_ID, 0.1);
    AbstractKMeans<DoubleVector, ?> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeansMinusMinus.class, params);
    testParameterizationOk(params);

    // run KMeans on database
    Clustering<?> result = kmeans.run(db);
    testFMeasure(db, result, 0.998);
    testClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }


  @Test
  public void testKMeansMinusMinusOutlier() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.K_ID, 5);
    params.addParameter(KMeans.SEED_ID, 7);
    params.addParameter(KMeansMinusMinus.Parameterizer.RATE_ID, 0.1);
    params.addFlag(KMeansMinusMinus.Parameterizer.NOISE_FLAG_ID);
    AbstractKMeans<DoubleVector, ?> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeansMinusMinus.class, params);
    testParameterizationOk(params);

    // run KMeans on database
    Clustering<?> result = kmeans.run(db);
    testFMeasure(db, result, 0.92674);
    testClusterSizes(result, new int[] { 100, 115, 185, 200, 200, 200 });
  }
}