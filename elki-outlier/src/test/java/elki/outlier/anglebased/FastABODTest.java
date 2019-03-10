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
package elki.outlier.anglebased;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.data.DoubleVector;
import elki.database.AbstractDatabase;
import elki.database.Database;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.distance.distancefunction.minkowski.EuclideanDistance;
import elki.distance.distancefunction.minkowski.SquaredEuclideanDistance;
import elki.distance.similarityfunction.kernel.LinearKernel;
import elki.distance.similarityfunction.kernel.PolynomialKernel;
import elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the FastABOD algorithm.
 *
 * @author Lucia Cichella
 * @since 0.7.0
 */
public class FastABODTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testFastABODLinear() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, LinearKernel.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODLinearIndexSquared() {
    ListParameterization pars = new ListParameterization();
    pars.addParameter(AbstractDatabase.Parameterizer.INDEX_ID, MaterializeKNNPreprocessor.Factory.class);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, 6);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960, pars);
    assertTrue(db.getKNNQuery(db.getDistanceQuery(db.getRelation(DoubleVector.FIELD), SquaredEuclideanDistance.STATIC), 5) instanceof PreprocessorKNNQuery);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, LinearKernel.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODLinearIndexEuclidean() {
    ListParameterization pars = new ListParameterization();
    pars.addParameter(AbstractDatabase.Parameterizer.INDEX_ID, MaterializeKNNPreprocessor.Factory.class);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, EuclideanDistance.STATIC);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, 6);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960, pars);
    assertTrue(db.getKNNQuery(db.getDistanceQuery(db.getRelation(DoubleVector.FIELD), EuclideanDistance.STATIC), 5) instanceof PreprocessorKNNQuery);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, LinearKernel.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODPoly1() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, new PolynomialKernel(1)) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODPoly2() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5).build().run(db);
    testAUC(db, "Noise", result, 0.94626962962);
    testSingleScore(result, 945, 3.28913914467E-4);
  }
}
