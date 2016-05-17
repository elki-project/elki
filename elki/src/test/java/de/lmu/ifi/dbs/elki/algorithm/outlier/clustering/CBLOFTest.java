package de.lmu.ifi.dbs.elki.algorithm.outlier.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansSort;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the CBLOF outlier detection algorithm.
 *
 * @author Patrick Kostjens
 */
public class CBLOFTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testCBLOFDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(CBLOF.Parameterizer.ALPHPA_ID, 0.8);
    params.addParameter(CBLOF.Parameterizer.BETA_ID, 3);
    params.addParameter(CBLOF.Parameterizer.CLUSTERING_ID, KMeansSort.class);
    params.addParameter(KMeans.K_ID, 1);

    // setup Algorithm
    CBLOF<DoubleVector> cblof = ClassGenericsUtil.parameterizeOrAbort(CBLOF.class, params);
    testParameterizationOk(params);

    OutlierResult result = cblof.run(db);

    testAUC(db, "Noise", result, 0.8232666666666666);
    testSingleScore(result, 416, 260.80721255987174);
  }
}