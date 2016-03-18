package de.lmu.ifi.dbs.elki.algorithm.outlier;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.AutotuningPCA;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.RANSACCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the COP algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.1
 */
public class COPTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testCOP() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(COP.Parameterizer.K_ID, 30);

    // setup Algorithm
    COP<DoubleVector> cop = ClassGenericsUtil.parameterizeOrAbort(COP.class, params);
    testParameterizationOk(params);

    OutlierResult result = cop.run(db);

    testAUC(db, "Noise", result, 0.89476666);
    testSingleScore(result, 416, 0.26795866);
  }

  @Test
  public void testCOPRobust() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(COP.Parameterizer.K_ID, 30);
    params.addParameter(COP.Parameterizer.PCARUNNER_ID, AutotuningPCA.class);
    params.addParameter(AutotuningPCA.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class);

    // setup Algorithm
    COP<DoubleVector> cop = ClassGenericsUtil.parameterizeOrAbort(COP.class, params);
    testParameterizationOk(params);

    OutlierResult result = cop.run(db);

    testAUC(db, "Noise", result, 0.90166666);
    testSingleScore(result, 416, 0.25705955);
  }

  @Test
  public void testCOPRANSAC() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(COP.Parameterizer.K_ID, 30);
    params.addParameter(COP.Parameterizer.PCARUNNER_ID, AutotuningPCA.class);
    params.addParameter(AutotuningPCA.Parameterizer.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class);
    params.addParameter(AutotuningPCA.Parameterizer.PCA_COVARIANCE_MATRIX, RANSACCovarianceMatrixBuilder.class);
    params.addParameter(RANSACCovarianceMatrixBuilder.Parameterizer.ITER_ID, 25);
    params.addParameter(RANSACCovarianceMatrixBuilder.Parameterizer.SEED_ID, 0);

    // setup Algorithm
    COP<DoubleVector> cop = ClassGenericsUtil.parameterizeOrAbort(COP.class, params);
    testParameterizationOk(params);

    OutlierResult result = cop.run(db);

    testAUC(db, "Noise", result, 0.89383333);
    testSingleScore(result, 416, 0.37358079);
  }
}