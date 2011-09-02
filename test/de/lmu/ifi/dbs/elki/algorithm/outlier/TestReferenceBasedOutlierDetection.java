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
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;

/**
 * Tests the ReferenceBasedOutlierDetection algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestReferenceBasedOutlierDetection extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testReferenceBasedOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ReferenceBasedOutlierDetection.K_ID, 11);
    params.addParameter(GridBasedReferencePoints.GRID_ID, 11);

    // setup Algorithm
    ReferenceBasedOutlierDetection<DoubleVector, DoubleDistance> referenceBasedOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(ReferenceBasedOutlierDetection.class, params);
    testParameterizationOk(params);

    // run ReferenceBasedOutlierDetection on database
    OutlierResult result = referenceBasedOutlierDetection.run(db);

    testSingleScore(result, 945, 0.9260829537195538);
    testAUC(db, "Noise", result, 0.9892407407407409);
  }
}