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
package elki.outlier;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.database.Database;
import elki.math.linearalgebra.pca.AutotuningPCA;
import elki.math.linearalgebra.pca.RANSACCovarianceMatrixBuilder;
import elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

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
        .with(COP.Par.K_ID, 30).build().autorun(db);
    assertAUC(db, "Noise", result, 0.89476666);
    assertSingleScore(result, 416, 0.26795866);
  }

  @Test
  public void testCOPChisquared() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Par.K_ID, 30) //
        .with(COP.Par.MODELS_ID) //
        .with(COP.Par.DIST_ID, COP.DistanceDist.CHISQUARED)//
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.897);
    assertSingleScore(result, 416, 0.0080449);
  }

  @Test
  public void testCOPRobust() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Par.K_ID, 30)//
        .with(COP.Par.PCARUNNER_ID, AutotuningPCA.class) //
        .with(AutotuningPCA.Par.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Par.WEIGHT_ID, ErfcWeight.class) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.90166666);
    assertSingleScore(result, 416, 0.25705955);
  }

  @Test
  public void testCOPRANSAC() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<COP<DoubleVector>>(COP.class)//
        .with(COP.Par.K_ID, 30)//
        .with(COP.Par.PCARUNNER_ID, AutotuningPCA.class) //
        .with(AutotuningPCA.Par.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class) //
        .with(AutotuningPCA.Par.PCA_COVARIANCE_MATRIX, RANSACCovarianceMatrixBuilder.class)//
        .with(RANSACCovarianceMatrixBuilder.Par.ITER_ID, 25) //
        .with(RANSACCovarianceMatrixBuilder.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.8866);
    assertSingleScore(result, 416, 0.27348);
  }
}
