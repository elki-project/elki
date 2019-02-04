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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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

    Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = db.getDistanceQuery(rel, EuclideanDistanceFunction.STATIC);

    // get linear queries
    LinearScanDistanceKNNQuery<DoubleVector> lin_knn_query = new LinearScanDistanceKNNQuery<>(distanceQuery);

    // get preprocessed queries
    NNDescent<DoubleVector> preproc = new ELKIBuilder<NNDescent.Factory<DoubleVector>>(NNDescent.Factory.class) //
        .with(NNDescent.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistanceFunction()) //
        .with(NNDescent.Factory.K_ID, k) //
        .with(NNDescent.Factory.Parameterizer.SEED_ID, 0) //
        .with(NNDescent.Factory.Parameterizer.DELTA_ID, 0.1) //
        .with(NNDescent.Factory.Parameterizer.RHO_ID, 0.5) //
        .build().instantiate(rel);
    KNNQuery<DoubleVector> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    // add as index
    db.getHierarchy().add(rel, preproc);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanDistanceKNNQuery);

    // test queries
    MaterializedKNNPreprocessorTest.testKNNQueries(rel, lin_knn_query, preproc_knn_query, k);
    // also test partial queries, forward only
    MaterializedKNNPreprocessorTest.testKNNQueries(rel, lin_knn_query, preproc_knn_query, k / 2);
  }
}
