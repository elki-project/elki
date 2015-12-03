package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

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
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the ALOCI algorithm.
 *
 * @author Lucia Cichella
 */
public class TestALOCI extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testLOCI() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ALOCI.Parameterizer.SEED_ID, 0);
    params.addParameter(ALOCI.Parameterizer.GRIDS_ID, 3);

    // setup Algorithm
    ALOCI<DoubleVector> aloci = ClassGenericsUtil.parameterizeOrAbort(ALOCI.class, params);
    testParameterizationOk(params);

    // run LOCI on database
    OutlierResult result = aloci.run(db);

    testAUC(db, "Noise", result, 0.77011111);
    testSingleScore(result, 146, 1.1242238186577);
  }
}