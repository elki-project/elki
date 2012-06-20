package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

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
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.WelchTTest;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the HiCS algorithm.
 * 
 * @author Erich Schubert
 */
public class TestHiCS extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testHiCSKS() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, 10);
    params.addParameter(HiCS.Parameterizer.LIMIT_ID, 10);
    params.addParameter(HiCS.Parameterizer.SEED_ID, 0);
    params.addParameter(HiCS.Parameterizer.TEST_ID, KolmogorovSmirnovTest.STATIC);

    // setup Algorithm
    HiCS<DoubleVector> fb = ClassGenericsUtil.parameterizeOrAbort(HiCS.class, params);
    testParameterizationOk(params);

    // run HiCS on database
    OutlierResult result = fb.run(db);

    testAUC(db, "Noise", result, 0.9024537815126049);
    testSingleScore(result, 1293, 5.0754391836);
  }

  @Test
  public void testHiCSWelch() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, 10);
    params.addParameter(HiCS.Parameterizer.LIMIT_ID, 10);
    params.addParameter(HiCS.Parameterizer.SEED_ID, 0);
    params.addParameter(HiCS.Parameterizer.TEST_ID, WelchTTest.STATIC);

    // setup Algorithm
    HiCS<DoubleVector> fb = ClassGenericsUtil.parameterizeOrAbort(HiCS.class, params);
    testParameterizationOk(params);

    // run HiCS on database
    OutlierResult result = fb.run(db);

    testAUC(db, "Noise", result, 0.6597983193);
    testSingleScore(result, 1293, 2.6993476951);
  }
}