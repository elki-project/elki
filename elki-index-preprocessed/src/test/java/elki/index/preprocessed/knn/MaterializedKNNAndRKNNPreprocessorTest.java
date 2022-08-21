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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.type.TypeUtil;
import elki.database.HashmapDatabase;
import elki.database.UpdatableDatabase;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.LinearScanKNNByDBID;
import elki.database.query.knn.LinearScanKNNByObject;
import elki.database.query.rknn.LinearScanRKNNByDBID;
import elki.database.query.rknn.LinearScanRKNNByObject;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.datasource.FileBasedDatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.distance.minkowski.EuclideanDistance;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;

/**
 * Test case to validate the dynamic updates of materialized kNN and RkNN
 * preprocessors.
 *
 * @author Elke Achtert
 * @since 0.7.0
 */
public class MaterializedKNNAndRKNNPreprocessorTest {
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
    UpdatableDatabase db = new ELKIBuilder<>(HashmapDatabase.class) //
        .with(FileBasedDatabaseConnection.Par.INPUT_ID, getClass().getClassLoader().getResource(dataset)) //
        .build();
    db.initialize();

    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = new QueryBuilder<>(relation, EuclideanDistance.STATIC).distanceQuery();

    // verify data set size.
    assertEquals("Data set size doesn't match parameters.", shoulds, relation.size());

    // get linear queries
    KNNSearcher<DBIDRef> lin_knn_query = new LinearScanKNNByDBID<>(distanceQuery);
    RKNNSearcher<DBIDRef> lin_rknn_query = new LinearScanRKNNByDBID<>(distanceQuery, lin_knn_query);

    // get preprocessed queries
    MaterializeKNNAndRKNNPreprocessor<DoubleVector> preproc = //
        new ELKIBuilder<MaterializeKNNAndRKNNPreprocessor.Factory<DoubleVector>>(MaterializeKNNAndRKNNPreprocessor.Factory.class) //
            .with(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistance()) //
            .with(MaterializeKNNPreprocessor.Factory.K_ID, k) //
            .build().instantiate(relation);
    KNNSearcher<DBIDRef> preproc_knn_query = preproc.kNNByDBID(distanceQuery, k, 0);
    RKNNSearcher<DBIDRef> preproc_rknn_query = preproc.rkNNByDBID(distanceQuery, k, 0);
    // add as index
    Metadata.hierarchyOf(relation).addChild(preproc);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanKNNByObject);
    assertFalse("Preprocessor rknn query class incorrect.", preproc_rknn_query instanceof LinearScanRKNNByObject);

    // test queries
    MaterializedKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);
    testRKNNQueries(relation, lin_rknn_query, preproc_rknn_query, k);
    // also test partial queries, forward only
    MaterializedKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k / 2);

    // insert new objects
    List<DoubleVector> insertions = new ArrayList<>();
    NumberVector.Factory<DoubleVector> o = RelationUtil.getNumberVectorFactory(relation);
    int dim = RelationUtil.dimensionality(relation);
    Random random = new Random(seed);
    for(int i = 0; i < updatesize; i++) {
      insertions.add(VectorUtil.randomVector(o, dim, random));
    }
    DBIDs deletions = db.insert(MultipleObjectsBundle.makeSimple(relation.getDataTypeInformation(), insertions));

    MaterializedKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);
    testRKNNQueries(relation, lin_rknn_query, preproc_rknn_query, k);
    // delete objects
    db.delete(deletions);
    MaterializedKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);
    testRKNNQueries(relation, lin_rknn_query, preproc_rknn_query, k);
  }

  public static void testRKNNQueries(Relation<DoubleVector> rep, RKNNSearcher<DBIDRef> lin_rknn_query, RKNNSearcher<DBIDRef> preproc_rknn_query, int k) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(rep.getDBIDs());
    for(DBIDIter it = sample.iter(); it.valid(); it.advance()) {
      DoubleDBIDList lin_rknn = lin_rknn_query.getRKNN(it, k);
      DoubleDBIDList pre_rknn = preproc_rknn_query.getRKNN(it, k);
      DoubleDBIDListIter lin = lin_rknn.iter(), pre = pre_rknn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance()) {
        assertTrue(DBIDUtil.equal(lin, pre) || lin.doubleValue() == pre.doubleValue());
      }
      assertEquals("rkNN sizes do not agree for k=" + k, lin_rknn.size(), pre_rknn.size());
      for(int j = 0; j < lin_rknn.size(); j++) {
        assertTrue("rkNNs of linear scan and preprocessor do not match!", DBIDUtil.equal(lin.seek(j), pre.seek(j)));
        assertEquals("rkNNs of linear scan and preprocessor do not match!", lin.seek(j).doubleValue(), pre.seek(j).doubleValue(), 0.);
      }
    }
  }
}
