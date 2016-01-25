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
 * Tests the LDF algorithm.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class LDFTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testLDF() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LDF.Parameterizer.K_ID, 10);
    params.addParameter(LDF.Parameterizer.H_ID, 1);

    // setup Algorithm
    LDF<DoubleVector> ldf = ClassGenericsUtil.parameterizeOrAbort(LDF.class, params);
    testParameterizationOk(params);

    // run LDF on database
    OutlierResult result = ldf.run(db);

    testSingleScore(result, 1293, 3.158819);
    testAUC(db, "Noise", result, 0.9127619);
  }
}