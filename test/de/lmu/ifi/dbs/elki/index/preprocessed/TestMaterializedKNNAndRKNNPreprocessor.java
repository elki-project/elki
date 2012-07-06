package de.lmu.ifi.dbs.elki.index.preprocessed;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.database.UpdatableDatabase;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNAndRKNNPreprocessor;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test case to validate the dynamic updates of materialized kNN and RkNN
 * preprocessors.
 * 
 * 
 * some index structures for accuracy. For a known data set and query point, the
 * top 10 nearest neighbors are queried and verified.
 * 
 * 
 * @author Elke Achtert
 */
public class TestMaterializedKNNAndRKNNPreprocessor implements JUnit4Test {
  // the following values depend on the data set used!
  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";

  // number of kNN to query
  int k = 10;

  // the size of objects inserted and deleted
  int updatesize = 12;

  int seed = 5;

  // size of the data set
  int shoulds = 330;

  /**
   * Actual test routine.
   * 
   * @throws ParameterException
   * @throws UnableToComplyException
   */
  @Test
  public void testPreprocessor() throws ParameterException, UnableToComplyException {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    // get database
    UpdatableDatabase db = ClassGenericsUtil.parameterizeOrAbort(HashmapDatabase.class, params);
    db.initialize();
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector, DoubleDistance> distanceQuery = db.getDistanceQuery(rep, EuclideanDistanceFunction.STATIC);

    // verify data set size.
    assertEquals("Data set size doesn't match parameters.", shoulds, rep.size());

    // get linear queries
    LinearScanKNNQuery<DoubleVector, DoubleDistance> lin_knn_query = new LinearScanKNNQuery<DoubleVector, DoubleDistance>(distanceQuery);
    LinearScanRKNNQuery<DoubleVector, DoubleDistance> lin_rknn_query = new LinearScanRKNNQuery<DoubleVector, DoubleDistance>(distanceQuery, lin_knn_query, k);

    // get preprocessed queries
    ListParameterization config = new ListParameterization();
    config.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistanceFunction());
    config.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, k);
    MaterializeKNNAndRKNNPreprocessor<DoubleVector, DoubleDistance> preproc = new MaterializeKNNAndRKNNPreprocessor<DoubleVector, DoubleDistance>(rep, distanceQuery.getDistanceFunction(), k);
    KNNQuery<DoubleVector, DoubleDistance> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    RKNNQuery<DoubleVector, DoubleDistance> preproc_rknn_query = preproc.getRKNNQuery(distanceQuery);
    // add as index
    db.addIndex(preproc);
    assertTrue("Preprocessor knn query class incorrect.", !(preproc_knn_query instanceof LinearScanKNNQuery));
    assertTrue("Preprocessor rknn query class incorrect.", !(preproc_rknn_query instanceof LinearScanKNNQuery));

    // test queries
    testKNNQueries(rep, lin_knn_query, preproc_knn_query, k);
    testRKNNQueries(rep, lin_rknn_query, preproc_rknn_query, k);
    // also test partial queries, forward only
    testKNNQueries(rep, lin_knn_query, preproc_knn_query, k / 2);

    // insert new objects
    List<DoubleVector> insertions = new ArrayList<DoubleVector>();
    DoubleVector o = DatabaseUtil.assumeVectorField(rep).getFactory();
    Random random = new Random(seed);
    for(int i = 0; i < updatesize; i++) {
      DoubleVector obj = VectorUtil.randomVector(o, random);
      insertions.add(obj);
    }
    System.out.println("Insert " + insertions);
    System.out.println();
    DBIDs deletions = db.insert(MultipleObjectsBundle.makeSimple(rep.getDataTypeInformation(), insertions));

    // test queries
    testKNNQueries(rep, lin_knn_query, preproc_knn_query, k);
    testRKNNQueries(rep, lin_rknn_query, preproc_rknn_query, k);

    // delete objects
    System.out.println("Delete " + deletions);
    db.delete(deletions);

    // test queries
    testKNNQueries(rep, lin_knn_query, preproc_knn_query, k);
    testRKNNQueries(rep, lin_rknn_query, preproc_rknn_query, k);
  }

  private void testKNNQueries(Relation<DoubleVector> rep, KNNQuery<DoubleVector, DoubleDistance> lin_knn_query, KNNQuery<DoubleVector, DoubleDistance> preproc_knn_query, int k) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(rep.getDBIDs());
    List<? extends KNNResult<DoubleDistance>> lin_knn_ids = lin_knn_query.getKNNForBulkDBIDs(sample, k);
    List<? extends KNNResult<DoubleDistance>> preproc_knn_ids = preproc_knn_query.getKNNForBulkDBIDs(sample, k);
    for(int i = 0; i < rep.size(); i++) {
      KNNResult<DoubleDistance> lin_knn = lin_knn_ids.get(i);
      KNNResult<DoubleDistance> pre_knn = preproc_knn_ids.get(i);
      DistanceDBIDResultIter<DoubleDistance> lin = lin_knn.iter(), pre = pre_knn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance(), i++) {
        if(!DBIDUtil.equal(lin, pre) && lin.getDistance().compareTo(pre.getDistance()) != 0) {
          System.out.print("LIN kNN #" + i + " " + lin.getDistancePair());
          System.out.print(" <=> ");
          System.out.print("PRE kNN #" + i + " " + pre.getDistancePair());
          System.out.println();
          break;
        }
      }
      assertEquals("kNN sizes do not agree.", lin_knn.size(), pre_knn.size());
      for(int j = 0; j < lin_knn.size(); j++) {
        assertTrue("kNNs of linear scan and preprocessor do not match!", DBIDUtil.equal(lin_knn.get(j), pre_knn.get(j)));
        assertTrue("kNNs of linear scan and preprocessor do not match!", lin_knn.get(j).getDistance().equals(pre_knn.get(j).getDistance()));
      }
    }
    System.out.println("knns ok");
  }

  private void testRKNNQueries(Relation<DoubleVector> rep, RKNNQuery<DoubleVector, DoubleDistance> lin_rknn_query, RKNNQuery<DoubleVector, DoubleDistance> preproc_rknn_query, int k) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(rep.getDBIDs());
    List<? extends DistanceDBIDResult<DoubleDistance>> lin_rknn_ids = lin_rknn_query.getRKNNForBulkDBIDs(sample, k);
    List<? extends DistanceDBIDResult<DoubleDistance>> preproc_rknn_ids = preproc_rknn_query.getRKNNForBulkDBIDs(sample, k);
    for(int i = 0; i < rep.size(); i++) {
      DistanceDBIDResult<DoubleDistance> lin_rknn = lin_rknn_ids.get(i);
      DistanceDBIDResult<DoubleDistance> pre_rknn = preproc_rknn_ids.get(i);

      DistanceDBIDResultIter<DoubleDistance> lin = lin_rknn.iter(), pre = pre_rknn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance(), i++) {
        if(!DBIDUtil.equal(lin, pre) || lin.getDistance().compareTo(pre.getDistance()) != 0) {
          System.out.print("LIN RkNN #" + i + " " + lin);
          System.out.print(" <=> ");
          System.out.print("PRE RkNN #" + i + " " + pre);
          System.out.println();
          break;
        }
      }
      assertEquals("rkNN sizes do not agree for k=" + k, lin_rknn.size(), pre_rknn.size());
      for(int j = 0; j < lin_rknn.size(); j++) {
        assertTrue("rkNNs of linear scan and preprocessor do not match!", DBIDUtil.equal(lin_rknn.get(j), pre_rknn.get(j)));
        assertTrue("rkNNs of linear scan and preprocessor do not match!", lin_rknn.get(j).getDistance().equals(pre_rknn.get(j).getDistance()));
      }
    }
    System.out.println("rknns ok");
    System.out.println();
  }
}