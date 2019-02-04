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
 * Test DOC on a simple test data set.
 *
 * On the first set, its an all-or-nothing depending on the parameters.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class DOCTest extends AbstractClusterAlgorithmTest {
  /**
   * Run DOC with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testDOCSimple() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<?> result = new ELKIBuilder<DOC<DoubleVector>>(DOC.class) //
        .with(DOC.Parameterizer.RANDOM_ID, 0) //
        .with(DOC.Parameterizer.ALPHA_ID, 0.4) //
        .with(DOC.Parameterizer.BETA_ID, 0.85) //
        .build().run(db);
    testFMeasure(db, result, 1.0);
    testClusterSizes(result, new int[] { 200, 400 });
  }

  /**
   * Run DOC with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testDOCOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<?> result = new ELKIBuilder<DOC<DoubleVector>>(DOC.class) //
        .with(DOC.Parameterizer.RANDOM_ID, 2) //
        .with(DOC.Parameterizer.ALPHA_ID, 0.4) //
        .with(DOC.Parameterizer.BETA_ID, 0.99) //
        .build().run(db);
    // Haven't found any working parameters for DOC on this data yet.
    testFMeasure(db, result, .5477386);
    testClusterSizes(result, new int[] { 850 });
  }
}
