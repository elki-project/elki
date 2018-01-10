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
package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full EM run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that EM's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.4.0
 */
public class EMTest extends AbstractClusterAlgorithmTest {
  /**
   * Run EM with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testEMResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.K_ID, 6) //
        .build().run(db);
    testFMeasure(db, result, 0.971834390);
    // This test case shows that EM can work very well,
    // but cause an empty cluster! This is OK.
    testClusterSizes(result, new int[] { 0, 5, 93, 100, 200, 312 });
  }

  /**
   * Run EM with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testEMResultsDiagonal() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.K_ID, 5) //
        .with(EM.Parameterizer.INIT_ID, DiagonalGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.9681384);
    testClusterSizes(result, new int[] { 7, 91, 99, 200, 313 });
  }

  /**
   * Run EM with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testEMResultsSpherical() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(EM.Parameterizer.K_ID, 4) //
        .with(EM.Parameterizer.INIT_ID, SphericalGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.811247176);
    testClusterSizes(result, new int[] { 8, 95, 198, 409 });
  }
}
