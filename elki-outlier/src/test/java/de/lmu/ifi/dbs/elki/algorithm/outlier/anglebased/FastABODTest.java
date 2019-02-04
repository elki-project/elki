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
package de.lmu.ifi.dbs.elki.algorithm.outlier.anglebased;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.LinearKernelFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

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
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, LinearKernelFunction.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODLinearIndexSquared() {
    ListParameterization pars = new ListParameterization();
    pars.addParameter(AbstractDatabase.Parameterizer.INDEX_ID, MaterializeKNNPreprocessor.Factory.class);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, SquaredEuclideanDistanceFunction.STATIC);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, 6);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960, pars);
    assertTrue(db.getKNNQuery(db.getDistanceQuery(db.getRelation(DoubleVector.FIELD), SquaredEuclideanDistanceFunction.STATIC), 5) instanceof PreprocessorKNNQuery);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, LinearKernelFunction.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODLinearIndexEuclidean() {
    ListParameterization pars = new ListParameterization();
    pars.addParameter(AbstractDatabase.Parameterizer.INDEX_ID, MaterializeKNNPreprocessor.Factory.class);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, EuclideanDistanceFunction.STATIC);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, 6);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960, pars);
    assertTrue(db.getKNNQuery(db.getDistanceQuery(db.getRelation(DoubleVector.FIELD), EuclideanDistanceFunction.STATIC), 5) instanceof PreprocessorKNNQuery);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, LinearKernelFunction.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.993814148);
    testSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODPoly1() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Parameterizer.K_ID, 5)//
        .with(FastABOD.Parameterizer.KERNEL_FUNCTION_ID, new PolynomialKernelFunction(1)) //
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
