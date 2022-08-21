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
package elki.index.preprocessed.knn;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDRef;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.LinearScanKNNByDBID;
import elki.database.query.knn.LinearScanKNNByObject;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for NNDescent
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class NNDescentTest {
  // the following values depend on the data set used!
  static String dataset = "elki/testdata/unittests/3clusters-and-noise-2d.csv";

  // number of kNN to query
  int k = 10;

  // the size of objects inserted and deleted
  int updatesize = 12;

  int seed = 5;

  // size of the data set
  int shoulds = 330;

  @Test
  public void testPreprocessor() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds);

    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = new QueryBuilder<>(relation, EuclideanDistance.STATIC).distanceQuery();

    // get linear queries
    KNNSearcher<DBIDRef> lin_knn_query = new LinearScanKNNByDBID<>(distanceQuery);

    // get preprocessed queries
    NNDescent<DoubleVector> preproc = new ELKIBuilder<NNDescent.Factory<DoubleVector>>(NNDescent.Factory.class) //
        .with(NNDescent.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistance()) //
        .with(NNDescent.Factory.K_ID, k) //
        .with(NNDescent.Factory.Par.SEED_ID, 1) //
        .with(NNDescent.Factory.Par.DELTA_ID, 0.1) //
        .with(NNDescent.Factory.Par.RHO_ID, 0.5) //
        .build().instantiate(relation);
    KNNSearcher<DBIDRef> preproc_knn_query = preproc.kNNByDBID(distanceQuery, k, 0);
    // add as index
    Metadata.hierarchyOf(relation).addChild(preproc);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanKNNByObject);

    // test queries
    MaterializedKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k, 37);
    // also test partial queries, forward only
    MaterializedKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k / 2, 10);
  }
}
