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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test P3C on a simple test data set.
 * 
 * Note: both data sets are really beneficial for P3C, and with reasonably
 * chosen parameters, it works perfectly.
 * 
 * FIXME: Previously, these test would score perfect. Now we have one outlier!
 * But from visual inspection, this might be a true positive.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class P3CTest extends AbstractSimpleAlgorithmTest {
  /**
   * Run P3C with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testP3CSimple() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();

    // setup algorithm
    P3C<DoubleVector> p3c = ClassGenericsUtil.parameterizeOrAbort(P3C.class, params);
    testParameterizationOk(params);

    // run P3C on database
    Clustering<?> result = p3c.run(db);

    testFMeasure(db, result, .99800101);
    testClusterSizes(result, new int[] { 1, 200, 399 });
  }

  /**
   * Run P3C with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testP3COverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(P3C.Parameterizer.ALPHA_THRESHOLD_ID, 0.01);
    P3C<DoubleVector> p3c = ClassGenericsUtil.parameterizeOrAbort(P3C.class, params);
    testParameterizationOk(params);

    // run P3C on database
    Clustering<?> result = p3c.run(db);
    testFMeasure(db, result, .99596185);
    testClusterSizes(result, new int[] { 4, 148, 300, 398 });
  }
}