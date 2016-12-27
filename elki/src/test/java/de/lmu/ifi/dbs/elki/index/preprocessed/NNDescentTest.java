package de.lmu.ifi.dbs.elki.index.preprocessed;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2016
 * Ludwig-Maximilians-Universität München
 * Lehr- und Forschungseinheit für Datenbanksysteme
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
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.NNDescent;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Regression test for NNDescent
 */
public class NNDescentTest {
  // the following values depend on the data set used!
  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";

  // number of kNN to query
  int k = 10;

  // the size of objects inserted and deleted
  int updatesize = 12;

  int seed = 5;

  // size of the data set
  int shoulds = 330;

  @Test
  public void testPreprocessor() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, null, null);

    Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = db.getDistanceQuery(rel, EuclideanDistanceFunction.STATIC);

    // get linear queries
    LinearScanDistanceKNNQuery<DoubleVector> lin_knn_query = new LinearScanDistanceKNNQuery<>(distanceQuery);

    // get preprocessed queries
    ListParameterization config = new ListParameterization();
    config.addParameter(NNDescent.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistanceFunction());
    config.addParameter(NNDescent.Factory.K_ID, k);
    config.addParameter(NNDescent.Factory.Parameterizer.SEED_ID, 0);
    config.addParameter(NNDescent.Factory.Parameterizer.DELTA_ID, 0.1);
    config.addParameter(NNDescent.Factory.Parameterizer.RHO_ID, 0.5);
    NNDescent.Factory<DoubleVector> preprocf = ClassGenericsUtil.parameterizeOrAbort(NNDescent.Factory.class, config);
    NNDescent<DoubleVector> preproc = preprocf.instantiate(rel);
    KNNQuery<DoubleVector> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    // add as index
    db.getHierarchy().add(rel, preproc);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanDistanceKNNQuery);

    // test queries
    testKNNQueries(rel, lin_knn_query, preproc_knn_query, k);
    // also test partial queries, forward only
    testKNNQueries(rel, lin_knn_query, preproc_knn_query, k / 2);
  }

  private void testKNNQueries(Relation<DoubleVector> rep, KNNQuery<DoubleVector> lin_knn_query, KNNQuery<DoubleVector> preproc_knn_query, int k) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(rep.getDBIDs());
    List<? extends KNNList> lin_knn_ids = lin_knn_query.getKNNForBulkDBIDs(sample, k);
    List<? extends KNNList> preproc_knn_ids = preproc_knn_query.getKNNForBulkDBIDs(sample, k);
    for(int i = 0; i < rep.size(); i++) {
      KNNList lin_knn = lin_knn_ids.get(i);
      KNNList pre_knn = preproc_knn_ids.get(i);
      DoubleDBIDListIter lin = lin_knn.iter(), pre = pre_knn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance(), i++) {
        if(DBIDUtil.equal(lin, pre) || lin.doubleValue() == pre.doubleValue()) {
          continue;
        }
        StringBuilder buf = new StringBuilder();
        buf.append("Neighbor distances do not agree: ");
        buf.append(lin_knn.toString());
        buf.append(" got: ");
        buf.append(pre_knn.toString());
        fail(buf.toString());
      }
      assertEquals("kNN sizes do not agree.", lin_knn.size(), pre_knn.size());
      for(int j = 0; j < lin_knn.size(); j++) {
        assertTrue("kNNs of linear scan and preprocessor do not match!", DBIDUtil.equal(lin_knn.get(j), pre_knn.get(j)));
        assertEquals("kNNs of linear scan and preprocessor do not match!", lin_knn.get(j).doubleValue(), pre_knn.get(j).doubleValue(), 0.);
      }
    }
  }
}
