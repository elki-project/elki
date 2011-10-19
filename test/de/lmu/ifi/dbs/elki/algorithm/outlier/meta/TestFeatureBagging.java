package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

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
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the Feature Bagging algorithm.
 * 
 * @author Erich Schubert
 */
public class TestFeatureBagging extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testFeatureBaggingSum() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.NUM_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.SEED_ID, 1);

    // setup Algorithm
    FeatureBagging fb = ClassGenericsUtil.parameterizeOrAbort(FeatureBagging.class, params);
    testParameterizationOk(params);

    // run AggarwalYuEvolutionary on database
    OutlierResult result = fb.run(db);

    testSingleScore(result, 1293, 11.8295414);
    testAUC(db, "Noise", result, 0.9066106);
  }

  @Test
  public void testFeatureBaggingBreadth() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.NUM_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.SEED_ID, 5);
    params.addFlag(FeatureBagging.Parameterizer.BREADTH_ID);

    // setup Algorithm
    FeatureBagging fb = ClassGenericsUtil.parameterizeOrAbort(FeatureBagging.class, params);
    testParameterizationOk(params);

    // run AggarwalYuEvolutionary on database
    OutlierResult result = fb.run(db);

    testSingleScore(result, 1293, 1.321709879);
    testAUC(db, "Noise", result, 0.884212);
  }
}