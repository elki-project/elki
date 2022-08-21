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
package elki.index.distancematrix;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDRef;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.relation.Relation;
import elki.distance.CosineDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the precomputed distance matrix.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class PrecomputedDistanceMatrixTest extends AbstractIndexStructureTest {
  // the following values depend on the data set used!
  final static String dataset = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  final static int shoulds = 600;

  @Test
  public void testEuclidean() {
    PrecomputedDistanceMatrix.Factory<?> factory = new ELKIBuilder<>(PrecomputedDistanceMatrix.Factory.class) //
        .with(PrecomputedDistanceMatrix.Factory.Par.DISTANCE_ID, EuclideanDistance.class).build();
    assertExactEuclidean(factory, PrecomputedDistanceMatrix.PrecomputedKNNQuery.class, PrecomputedDistanceMatrix.PrecomputedRangeQuery.class, true);
    assertPrioritySearchEuclidean(factory, PrecomputedDistanceMatrix.PrecomputedDistancePrioritySearcher.class, true);
    assertSinglePoint(factory, PrecomputedDistanceMatrix.PrecomputedKNNQuery.class, PrecomputedDistanceMatrix.PrecomputedRangeQuery.class);
  }

  /**
   * This test is to validate the odd optimized sorting logic for partial search
   * based on the distance matrix, which tries to combine benefits of
   * QuickSelect and QuickSort.
   */
  @Test
  public void testPrioritySorting() {
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(StaticArrayDatabase.Par.INDEX_ID, PrecomputedDistanceMatrix.Factory.class) //
        .addParameter(PrecomputedDistanceMatrix.Factory.Par.DISTANCE_ID, EuclideanDistance.class);
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    PrioritySearcher<DBIDRef> prioq = new QueryBuilder<>(relation, EuclideanDistance.STATIC).cheapOnly().priorityByDBID();
    double lastd = 0.0;
    int i = 0;
    for(prioq.search(relation.iterDBIDs()); prioq.valid(); prioq.advance(), i++) {
      double dist = prioq.computeExactDistance();
      assertTrue("Not increasing at " + i, dist >= lastd);
      lastd = dist;
    }
  }

  @Test
  public void testCosine() {
    PrecomputedDistanceMatrix.Factory<?> factory = new ELKIBuilder<>(PrecomputedDistanceMatrix.Factory.class) //
        .with(PrecomputedDistanceMatrix.Factory.Par.DISTANCE_ID, CosineDistance.class).build();
    assertExactCosine(factory, PrecomputedDistanceMatrix.PrecomputedKNNQuery.class, PrecomputedDistanceMatrix.PrecomputedRangeQuery.class, true);
  }
}
