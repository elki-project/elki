package de.lmu.ifi.dbs.elki.algorithm.outlier;

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
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the LDOF algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestLDOF extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testLDOF() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LDOF.K_ID, 25);

    // setup Algorithm
    LDOF<DoubleVector, DoubleDistance> ldof = ClassGenericsUtil.parameterizeOrAbort(LDOF.class, params);
    testParameterizationOk(params);

    // run LDOF on database
    OutlierResult result = ldof.run(db);

    testAUC(db, "Noise", result, 0.9637948717948718);
    testSingleScore(result, 1025, 0.8976268846182947);
  }
}