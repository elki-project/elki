package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full PROCLUS run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that PROCLUS performance
 * doesn't unexpectedly drop on this data set (and also ensures that the
 * algorithms work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestPROCLUSResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run PROCLUS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPROCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(PROCLUS.Parameterizer.L_ID, 1);
    params.addParameter(PROCLUS.Parameterizer.K_ID, 4);
    params.addParameter(PROCLUS.Parameterizer.SEED_ID, 2);

    // setup algorithm
    PROCLUS<DoubleVector> proclus = ClassGenericsUtil.parameterizeOrAbort(PROCLUS.class, params);
    testParameterizationOk(params);

    // run PROCLUS on database
    Clustering<?> result = proclus.run(db);

    testFMeasure(db, result, 0.900947932);
    testClusterSizes(result, new int[] { 15, 35, 200, 350 });
  }

  /**
   * Run PROCLUS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPROCLUSSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(PROCLUS.Parameterizer.L_ID, 2);
    params.addParameter(PROCLUS.Parameterizer.K_ID, 3);
    params.addParameter(PROCLUS.Parameterizer.SEED_ID, 0);
    PROCLUS<DoubleVector> proclus = ClassGenericsUtil.parameterizeOrAbort(PROCLUS.class, params);
    testParameterizationOk(params);

    // run PROCLUS on database
    Clustering<?> result = proclus.run(db);
    testFMeasure(db, result, 0.739931511);
    testClusterSizes(result, new int[] { 146, 259, 445 });
  }
}