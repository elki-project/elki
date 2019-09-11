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
package elki.index.preprocessed.knn;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.type.TypeUtil;
import elki.database.AbstractDatabase;
import elki.database.HashmapDatabase;
import elki.database.UpdatableDatabase;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.knn.LinearScanDistanceKNNQuery;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.datasource.InputStreamDatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.parser.NumberVectorLabelParser;
import elki.distance.minkowski.EuclideanDistance;
import elki.result.Metadata;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test case to validate the dynamic updates of materialized kNN preprocessors.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MaterializedKNNPreprocessorTest {
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
    UpdatableDatabase db;
    // get database
    try (InputStream is = AbstractSimpleAlgorithmTest.open(dataset)) {
      ListParameterization params = new ListParameterization();
      // Setup parser and data loading
      NumberVectorLabelParser<DoubleVector> parser = new NumberVectorLabelParser<>(DoubleVector.FACTORY);
      InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, new ArrayList<>(), parser);

      // We want to allow the use of indexes via "params"
      params.addParameter(AbstractDatabase.Par.DATABASE_CONNECTION_ID, dbc);
      db = ClassGenericsUtil.parameterizeOrAbort(HashmapDatabase.class, params);
      db.initialize();
    }
    catch(IOException e) {
      fail("Test data " + dataset + " not found.");
      return;
    }

    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = relation.getDistanceQuery(EuclideanDistance.STATIC);

    // verify data set size.
    assertEquals("Data set size doesn't match parameters.", shoulds, relation.size());

    // get linear queries
    LinearScanDistanceKNNQuery<DoubleVector> lin_knn_query = new LinearScanDistanceKNNQuery<>(distanceQuery);

    // get preprocessed queries
    MaterializeKNNPreprocessor<DoubleVector> preproc = //
        new ELKIBuilder<MaterializeKNNPreprocessor.Factory<DoubleVector>>(MaterializeKNNPreprocessor.Factory.class) //
            .with(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistance()) //
            .with(MaterializeKNNPreprocessor.Factory.K_ID, k) //
            .build().instantiate(relation);
    KNNQuery<DoubleVector> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    // add as index
    Metadata.hierarchyOf(relation).addChild(preproc);

    // test queries
    testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);
    // also test partial queries, forward only
    testKNNQueries(relation, lin_knn_query, preproc_knn_query, k / 2);

    // insert new objects
    List<DoubleVector> insertions = new ArrayList<>();
    NumberVector.Factory<DoubleVector> o = RelationUtil.getNumberVectorFactory(relation);
    int dim = RelationUtil.dimensionality(relation);
    Random random = new Random(seed);
    for(int i = 0; i < updatesize; i++) {
      DoubleVector obj = VectorUtil.randomVector(o, dim, random);
      insertions.add(obj);
    }
    // System.out.println("Insert " + insertions);
    DBIDs deletions = db.insert(MultipleObjectsBundle.makeSimple(relation.getDataTypeInformation(), insertions));

    // test queries
    testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);

    // delete objects
    // System.out.println("Delete " + deletions);
    db.delete(deletions);

    // test queries
    testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);
  }

  public static void testKNNQueries(Relation<DoubleVector> rep, KNNQuery<DoubleVector> lin_knn_query, KNNQuery<DoubleVector> preproc_knn_query, int k) {
    assertNotEquals("Preprocessor knn query class incorrect.", lin_knn_query.getClass(), preproc_knn_query.getClass());
    for(DBIDIter iter = rep.iterDBIDs(); iter.valid(); iter.advance()) {
      KNNList lin_knn = lin_knn_query.getKNNForDBID(iter, k);
      KNNList pre_knn = preproc_knn_query.getKNNForDBID(iter, k);
      DoubleDBIDListIter lin = lin_knn.iter(), pre = pre_knn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance()) {
        if(DBIDUtil.equal(lin, pre) || lin.doubleValue() == pre.doubleValue()) {
          continue;
        }
        fail(new StringBuilder(1000).append("Neighbor distances do not agree: #")//
            .append(iter).append(' ') //
            .append(lin_knn.toString()).append(" got: ").append(pre_knn.toString()).toString());
      }
      assertEquals("kNN sizes do not agree.", lin_knn.size(), pre_knn.size());
      for(int j = 0; j < lin_knn.size(); j++) {
        assertTrue("kNNs of linear scan and preprocessor do not match!", DBIDUtil.equal(lin.seek(j), pre.seek(j)));
        assertEquals("kNNs of linear scan and preprocessor do not match!", lin.seek(j).doubleValue(), pre.seek(j).doubleValue(), 0.);
      }
    }
  }

  public static void testKNNQueries(Relation<DoubleVector> rep, KNNQuery<DoubleVector> lin_knn_query, KNNQuery<DoubleVector> preproc_knn_query, int k, int allowed_errors) {
    for(DBIDIter iter = rep.iterDBIDs(); iter.valid(); iter.advance()) {
      KNNList lin_knn = lin_knn_query.getKNNForDBID(iter, k);
      KNNList pre_knn = preproc_knn_query.getKNNForDBID(iter, k);
      DoubleDBIDListIter lin = lin_knn.iter(), pre = pre_knn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance()) {
        if(DBIDUtil.equal(lin, pre) || lin.doubleValue() == pre.doubleValue()) {
          continue;
        }
        if(--allowed_errors < 0) {
          fail(new StringBuilder(1000).append("Neighbor distances do not agree: #")//
              .append(iter).append(' ') //
              .append(lin_knn.toString()).append(" got: ").append(pre_knn.toString()).toString());
        }
      }
      assertEquals("kNN sizes do not agree.", lin_knn.size(), pre_knn.size());
    }
    assertEquals("Error budget is not tight, remaining errors: ", 0, allowed_errors);
  }
}
