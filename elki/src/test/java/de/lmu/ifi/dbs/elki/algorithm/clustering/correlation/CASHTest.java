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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Perform a full CASH run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that CASH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class CASHTest extends AbstractClusterAlgorithmTest {
  /**
   * Run CASH with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testCASHResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);
    Clustering<Model> result = new ELKIBuilder<CASH<DoubleVector>>(CASH.class) //
        .with(CASH.Parameterizer.JITTER_ID, 0.7) //
        .with(CASH.Parameterizer.MINPTS_ID, 50) //
        .with(CASH.Parameterizer.MAXLEVEL_ID, 25) //
        .with(CASH.Parameterizer.ADJUST_ID) //
        .build().run(db);
    testFMeasure(db, result, 0.50074); // with hierarchical pairs: 0.64102
    testClusterSizes(result, new int[] { 18, 80, 252, 468 });
  }

  /**
   * Run CASH with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testCASHEmbedded() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-embedded-2-4d.ascii", 600);
    Clustering<Model> result = new ELKIBuilder<CASH<DoubleVector>>(CASH.class) //
        .with(CASH.Parameterizer.JITTER_ID, 0.7) //
        .with(CASH.Parameterizer.MINPTS_ID, 160) //
        .with(CASH.Parameterizer.MAXLEVEL_ID, 40) //
        .build().run(db);
    testFMeasure(db, result, 0.443246);
    testClusterSizes(result, new int[] { 169, 196, 235 });
  }
}
