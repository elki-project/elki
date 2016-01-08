package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test DOC on a simple test data set.
 *
 * On the first set, its an all-or-nothing depending on the parameters.
 *
 * @author Erich Schubert
 */
public class DOCTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run DOC with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testDOCSimple() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(DOC.Parameterizer.RANDOM_ID, 0);
    params.addParameter(DOC.Parameterizer.ALPHA_ID, 0.4);
    params.addParameter(DOC.Parameterizer.BETA_ID, 0.85);

    // setup algorithm
    DOC<DoubleVector> doc = ClassGenericsUtil.parameterizeOrAbort(DOC.class, params);
    testParameterizationOk(params);

    // run DOC on database
    Clustering<?> result = doc.run(db);

    testClusterSizes(result, new int[] { 200, 400 });
    testFMeasure(db, result, 1.0);
  }

  /**
   * Run DOC with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testDOCOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DOC.Parameterizer.RANDOM_ID, 0);
    params.addParameter(DOC.Parameterizer.ALPHA_ID, 0.4);
    params.addParameter(DOC.Parameterizer.BETA_ID, 0.95);
    params.addFlag(DOC.Parameterizer.HEURISTICS_ID);
    params.addParameter(DOC.Parameterizer.D_ZERO_ID, 1);

    DOC<DoubleVector> doc = ClassGenericsUtil.parameterizeOrAbort(DOC.class, params);
    testParameterizationOk(params);

    // run DOC on database
    Clustering<?> result = doc.run(db);
    testFMeasure(db, result, .54271816);
    testClusterSizes(result, new int[] { 1, 20, 33, 40, 56, 104, 274, 322 });
  }
}