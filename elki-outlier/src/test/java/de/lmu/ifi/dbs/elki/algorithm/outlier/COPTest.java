/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.AutotuningPCA;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.RANSACCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the COP algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class COPTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testCOP() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Parameterizer.K_ID, 30).build().run(db);
    testAUC(db, "Noise", result, 0.89476666);
    testSingleScore(result, 416, 0.26795866);
  }

  @Test
  public void testCOPChisquared() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Parameterizer.K_ID, 30) //
        .with(COP.Parameterizer.MODELS_ID) //
        .with(COP.Parameterizer.DIST_ID, COP.DistanceDist.CHISQUARED)//
        .build().run(db);
    testAUC(db, "Noise", result, 0.897);
    testSingleScore(result, 416, 0.0080449);
  }

  @Test
  public void testCOPRobust() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Parameterizer.K_ID, 30)//
        .with(COP.Parameterizer.PCARUNNER_ID, AutotuningPCA.class) //
        .with(AutotuningPCA.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.90166666);
    testSingleScore(result, 416, 0.25705955);
  }

  @Test
  public void testCOPRANSAC() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Parameterizer.K_ID, 30)//
        .with(COP.Parameterizer.PCARUNNER_ID, AutotuningPCA.class) //
        .with(AutotuningPCA.Parameterizer.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class) //
        .with(AutotuningPCA.Parameterizer.PCA_COVARIANCE_MATRIX, RANSACCovarianceMatrixBuilder.class)//
        .with(RANSACCovarianceMatrixBuilder.Parameterizer.ITER_ID, 25) //
        .with(RANSACCovarianceMatrixBuilder.Parameterizer.SEED_ID, 0) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.8993);
    testSingleScore(result, 416, 0.410516);
  }
}
