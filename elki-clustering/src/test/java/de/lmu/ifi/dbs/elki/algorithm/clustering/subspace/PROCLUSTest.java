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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full PROCLUS run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that PROCLUS performance
 * doesn't unexpectedly drop on this data set (and also ensures that the
 * algorithms work, as a side effect).
 *
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PROCLUSTest extends AbstractClusterAlgorithmTest {
  /**
   * Run PROCLUS with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testPROCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<?> result = new ELKIBuilder<PROCLUS<DoubleVector>>(PROCLUS.class) //
        .with(PROCLUS.Parameterizer.L_ID, 1) //
        .with(PROCLUS.Parameterizer.K_ID, 4) //
        // NOTE: PROCLUS quality heavily depends on random...
        .with(PROCLUS.Parameterizer.SEED_ID, 12) //
        .build().run(db);
    testFMeasure(db, result, 0.88499877);
    testClusterSizes(result, new int[] { 22, 36, 200, 342 });
  }

  /**
   * Run PROCLUS with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testPROCLUSSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<?> result = new ELKIBuilder<PROCLUS<DoubleVector>>(PROCLUS.class) //
        .with(PROCLUS.Parameterizer.L_ID, 2) //
        .with(PROCLUS.Parameterizer.K_ID, 3) //
        // NOTE: PROCLUS quality heavily depends on random...
        .with(PROCLUS.Parameterizer.SEED_ID, 3) //
        .build().run(db);
    testFMeasure(db, result, 0.96985144);
    testClusterSizes(result, new int[] { 150, 288, 412 });
  }
}
