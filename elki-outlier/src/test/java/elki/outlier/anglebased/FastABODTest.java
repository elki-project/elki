/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.database.AbstractDatabase;
import elki.database.Database;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.similarity.kernel.LinearKernel;
import elki.similarity.kernel.PolynomialKernel;
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
        .with(FastABOD.Par.K_ID, 5)//
        .with(FastABOD.Par.KERNEL_FUNCTION_ID, LinearKernel.STATIC) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.993814148);
    assertSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODLinearIndexSquared() {
    ListParameterization pars = new ListParameterization();
    pars.addParameter(AbstractDatabase.Par.INDEX_ID, MaterializeKNNPreprocessor.Factory.class);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.STATIC);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, 6);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960, pars);
    Relation<NumberVector> relation = db.getRelation(DoubleVector.FIELD);
    assertTrue(new QueryBuilder<>(relation, SquaredEuclideanDistance.STATIC).kNNByDBID(5) instanceof PreprocessorKNNQuery);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Par.K_ID, 5)//
        .with(FastABOD.Par.KERNEL_FUNCTION_ID, LinearKernel.STATIC) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.993814148);
    assertSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODLinearIndexEuclidean() {
    ListParameterization pars = new ListParameterization();
    pars.addParameter(AbstractDatabase.Par.INDEX_ID, MaterializeKNNPreprocessor.Factory.class);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, EuclideanDistance.STATIC);
    pars.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, 6);
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960, pars);
    Relation<NumberVector> relation = db.getRelation(DoubleVector.FIELD);
    assertTrue(new QueryBuilder<>(relation, EuclideanDistance.STATIC).kNNByDBID(5) instanceof PreprocessorKNNQuery);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Par.K_ID, 5)//
        .with(FastABOD.Par.KERNEL_FUNCTION_ID, LinearKernel.STATIC) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.993814148);
    assertSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODPoly1() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Par.K_ID, 5)//
        .with(FastABOD.Par.KERNEL_FUNCTION_ID, new PolynomialKernel(1)) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.993814148);
    assertSingleScore(result, 945, 0.498653289);
  }

  @Test
  public void testFastABODPoly2() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<FastABOD<DoubleVector>>(FastABOD.class) //
        .with(FastABOD.Par.K_ID, 5).build().autorun(db);
    assertAUC(db, "Noise", result, 0.94626962962);
    assertSingleScore(result, 945, 3.28913914467E-4);
  }
}
