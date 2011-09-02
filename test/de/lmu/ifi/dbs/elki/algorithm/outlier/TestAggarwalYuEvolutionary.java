package de.lmu.ifi.dbs.elki.algorithm.outlier;

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
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the AggarwalYuEvolutionary algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestAggarwalYuEvolutionary extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testAggarwalYuEvolutionary() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuEvolutionary.K_ID, 2);
    params.addParameter(AggarwalYuEvolutionary.PHI_ID, 8);
    params.addParameter(AggarwalYuEvolutionary.M_ID, 5);
    params.addParameter(AggarwalYuEvolutionary.SEED_ID, 0);

    // setup Algorithm
    AggarwalYuEvolutionary<DoubleVector> aggarwalYuEvolutionary = ClassGenericsUtil.parameterizeOrAbort(AggarwalYuEvolutionary.class, params);
    testParameterizationOk(params);

    // run AggarwalYuEvolutionary on database
    OutlierResult result = aggarwalYuEvolutionary.run(db);

    testSingleScore(result, 945, 16.6553612449883);
    testAUC(db, "Noise", result, 0.5799537037037);
  }
}