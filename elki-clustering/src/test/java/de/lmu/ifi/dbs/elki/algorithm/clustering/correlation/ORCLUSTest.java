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
 * Performs a full ORCLUS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that ORCLUS performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Elke Achtert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class ORCLUSTest extends AbstractClusterAlgorithmTest {
  /**
   * Run ORCLUS with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testORCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-hierarchy.csv", 450);
    Clustering<Model> result = new ELKIBuilder<ORCLUS<DoubleVector>>(ORCLUS.class) //
        .with(ORCLUS.Parameterizer.K_ID, 3) //
        .with(ORCLUS.Parameterizer.L_ID, 1) //
        .with(ORCLUS.Parameterizer.SEED_ID, 1) //
        .build().run(db);
    testFMeasure(db, result, 0.627537295);
    testClusterSizes(result, new int[] { 25, 34, 391 });
  }

  /**
   * Run ORCLUS with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testORCLUSSkewedDisjoint() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-skewed-disjoint-3-5d.ascii", 601);
    Clustering<Model> result = new ELKIBuilder<ORCLUS<DoubleVector>>(ORCLUS.class) //
        .with(ORCLUS.Parameterizer.K_ID, 3) //
        .with(ORCLUS.Parameterizer.L_ID, 4) //
        .with(ORCLUS.Parameterizer.SEED_ID, 0) //
        .build().run(db);
    testFMeasure(db, result, 0.848054);
    testClusterSizes(result, new int[] { 189, 200, 212 });
  }
}
