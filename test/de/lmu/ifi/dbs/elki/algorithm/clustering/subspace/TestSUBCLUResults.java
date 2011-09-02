package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;
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
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SUBCLU run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SUBCLU performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestSUBCLUResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run SUBCLU with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testSUBCLUResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(SUBCLU.EPSILON_ID, 0.001);
    params.addParameter(SUBCLU.MINPTS_ID, 100);

    // setup algorithm
    SUBCLU<DoubleVector> subclu = ClassGenericsUtil.parameterizeOrAbort(SUBCLU.class, params);
    testParameterizationOk(params);

    // run SUBCLU on database
    Clustering<SubspaceModel<DoubleVector>> result = subclu.run(db);

    testFMeasure(db, result, 0.9090);
    testClusterSizes(result, new int[] { 191, 194, 395 });
  }

  /**
   * Run SUBCLU with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testSUBCLUSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
  
    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(SUBCLU.EPSILON_ID, 0.04);
    params.addParameter(SUBCLU.MINPTS_ID, 70);
    SUBCLU<DoubleVector> subclu = ClassGenericsUtil.parameterizeOrAbort(SUBCLU.class, params);
    testParameterizationOk(params);
  
    // run SUBCLU on database
    Clustering<SubspaceModel<DoubleVector>> result = subclu.run(db);
    testFMeasure(db, result, 0.49279033);
    testClusterSizes(result, new int[] { 99, 247, 303, 323, 437, 459 });
  }
}