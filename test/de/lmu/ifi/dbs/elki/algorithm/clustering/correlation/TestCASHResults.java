package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.parser.ParameterizationFunctionLabelParser;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full CASH run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that CASH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * @author Katharina Rausch
 */
public class TestCASHResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run CASH with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCASHResults() {
    ListParameterization inp = new ListParameterization();
    // CASH input
    inp.addParameter(FileBasedDatabaseConnection.PARSER_ID, ParameterizationFunctionLabelParser.class);
    // Input
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600, inp, null);

    // CASH parameters
    ListParameterization params = new ListParameterization();
    params.addParameter(CASH.JITTER_ID, 0.7);
    params.addParameter(CASH.MINPTS_ID, 50);
    params.addParameter(CASH.MAXLEVEL_ID, 25);
    params.addFlag(CASH.ADJUST_ID);

    // setup algorithm
    CASH cash = ClassGenericsUtil.parameterizeOrAbort(CASH.class, params);
    testParameterizationOk(params);

    // run CASH on database
    Clustering<Model> result = cash.run(db);

    testFMeasure(db, result, 0.49055); // with hierarchical pairs: 0.64102
    testClusterSizes(result, new int[] { 37, 71, 76, 442 });
  }

  /**
   * Run CASH with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCASHEmbedded() {
    // CASH input
    ListParameterization inp = new ListParameterization();
    inp.addParameter(FileBasedDatabaseConnection.PARSER_ID, ParameterizationFunctionLabelParser.class);
    Database db = makeSimpleDatabase(UNITTEST + "correlation-embedded-2-4d.ascii", 600, inp, null);

    // CASH parameters
    ListParameterization params = new ListParameterization();
    params.addParameter(CASH.JITTER_ID, 0.7);
    params.addParameter(CASH.MINPTS_ID, 160);
    params.addParameter(CASH.MAXLEVEL_ID, 40);

    // setup algorithm
    CASH cash = ClassGenericsUtil.parameterizeOrAbort(CASH.class, params);
    testParameterizationOk(params);

    // run CASH on database
    Clustering<Model> result = cash.run(db);
    testFMeasure(db, result, 0.443246);
    testClusterSizes(result, new int[] { 169, 196, 235 });
  }
}