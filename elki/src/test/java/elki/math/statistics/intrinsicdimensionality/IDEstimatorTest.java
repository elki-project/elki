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
package elki.math.statistics.intrinsicdimensionality;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.database.AbstractDatabase;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.query.QueryBuilder;
import elki.database.relation.Relation;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.ELKIBuilder;

/**
 * Test various ID estimators again.
 * <p>
 * Note: we cannot move this test to the math package, because we do not want to
 * have it depend on the database and queries to avoid circular dependencies.
 * <p>
 * TODO: also test the range query version.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class IDEstimatorTest {
  @Test
  public void testHillEstimator() {
    regressionTest(HillEstimator.STATIC, 5, 1000, 200, 1L, 5.19585175);
  }

  @Test
  public void testAggregatedHillEstimator() {
    regressionTest(AggregatedHillEstimator.STATIC, 5, 1000, 200, 1L, 5.16536175);
  }

  @Test
  public void testZipfEstimator() {
    regressionTest(ZipfEstimator.STATIC, 5, 1000, 200, 1L, 5.12688772);
  }

  @Test
  public void testMOMEstimator() {
    regressionTest(MOMEstimator.STATIC, 5, 1000, 200, 1L, 5.19465956);
  }

  @Test
  public void testGEDEstimator() {
    regressionTest(GEDEstimator.STATIC, 5, 1000, 200, 1L, 5.19002855);
  }

  @Test
  public void testRVEstimator() {
    regressionTest(RVEstimator.STATIC, 5, 1000, 200, 1L, 5.20562387);
  }

  @Test
  public void testPWMEstimator() {
    regressionTest(PWMEstimator.STATIC, 5, 1000, 200, 1L, 5.170590689);
  }

  @Test
  public void testPWM2Estimator() {
    regressionTest(PWM2Estimator.STATIC, 5, 1000, 200, 1L, 5.13632385);
  }

  @Test
  public void testLMomentsEstimator() {
    regressionTest(LMomentsEstimator.STATIC, 5, 1000, 200, 1L, 5.17598016);
  }

  /**
   * Regression test an estimator both with zeros and without.
   * 
   * @param est Estimator to test
   * @param dim Dimensionality to simulate
   * @param size Data size
   * @param k k Parameter
   * @param seed Random seed
   * @param edim Dimensionality the estimator is known to return.
   */
  protected static void regressionTest(IntrinsicDimensionalityEstimator<? super DoubleVector> est, int dim, int size, int k, long seed, double edim) {
    Random r = new Random(seed);
    double[][] data = new double[size][dim];
    // Note: query point at 0.
    // FIXME: generate some embedding instead?
    // TODO: add some 0 values, duplicate points.
    // Use some constant data set instead?
    for(int i = 1; i < size; i++) {
      double[] row = data[i];
      for(int j = 0; j < dim; j++) {
        row[j] = r.nextDouble() - .5;
      }
    }
    Database db = new ELKIBuilder<>(StaticArrayDatabase.class) //
        .with(AbstractDatabase.Par.DATABASE_CONNECTION_ID, new ArrayAdapterDatabaseConnection(data)) //
        .build();
    db.initialize();
    Relation<DoubleVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    QueryBuilder<DoubleVector> qb = new QueryBuilder<>(rel, EuclideanDistance.STATIC);
    assertEquals("Accuracy of " + est.getClass().getSimpleName(), edim, est.estimate(qb.kNNByDBID(), qb.distanceQuery(), rel.iterDBIDs(), k), 1e-8);
  }
}
