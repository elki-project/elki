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
package de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the IDOS outlier detection algorithm.
 * 
 * @author Erich Schubert
 */
public class IDOSTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testToyExample() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(IDOS.Parameterizer.KC_ID, 100);
    params.addParameter(IDOS.Parameterizer.KR_ID, 20);

    // setup Algorithm
    IDOS<DoubleVector> idos = ClassGenericsUtil.parameterizeOrAbort(IDOS.class, params);
    testParameterizationOk(params);

    // run KNNOutlier on database
    OutlierResult result = idos.run(db);

    testAUC(db, "Noise", result, 0.9194814);
    testSingleScore(result, 945, 1.130819);
  }
}