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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.metrical.covertree.CoverTree;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the DBOutlierDetection algorithm.
 * 
 * @author Lucia Cichella
 * @since 0.4.0
 */
public class DBOutlierDetectionTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testDBOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(DBOutlierDetection.Parameterizer.D_ID, 0.175);
    params.addParameter(DBOutlierDetection.Parameterizer.P_ID, 0.98);

    // setup Algorithm
    DBOutlierDetection<DoubleVector> dbOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(DBOutlierDetection.class, params);
    testParameterizationOk(params);

    // run DBOutlierDetection on database
    OutlierResult result = dbOutlierDetection.run(db);

    testSingleScore(result, 1025, 0.0);
    testAUC(db, "Noise", result, 0.97487179);
  }

  @Test
  public void testDBOutlierDetectionIndex() {
    ListParameterization iparams = new ListParameterization();
    iparams.addParameter(AbstractDatabase.Parameterizer.INDEX_ID, CoverTree.Factory.class);
    iparams.addParameter(CoverTree.Factory.Parameterizer.DISTANCE_FUNCTION_ID, EuclideanDistanceFunction.class);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025, iparams, null);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(DBOutlierDetection.Parameterizer.D_ID, 0.175);
    params.addParameter(DBOutlierDetection.Parameterizer.P_ID, 0.98);

    // setup Algorithm
    DBOutlierDetection<DoubleVector> dbOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(DBOutlierDetection.class, params);
    testParameterizationOk(params);

    // run DBOutlierDetection on database
    OutlierResult result = dbOutlierDetection.run(db);

    testSingleScore(result, 1025, 0.0);
    testAUC(db, "Noise", result, 0.97487179);
  }
}
