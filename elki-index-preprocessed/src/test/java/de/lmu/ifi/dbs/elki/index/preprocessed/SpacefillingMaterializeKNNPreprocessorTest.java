/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.index.preprocessed;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.SpacefillingMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.BinarySplitSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Regression test for space-filling curve NN search
 */
public class SpacefillingMaterializeKNNPreprocessorTest {
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
    ListParameterization config = new ListParameterization();
    config.addParameter(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.CURVES_ID, //
        HilbertSpatialSorter.class.getName() + "," + PeanoSpatialSorter.class.getName() + "," //
            + ZCurveSpatialSorter.class.getName() + "," + BinarySplitSpatialSorter.class.getName());
    config.addParameter(SpacefillingMaterializeKNNPreprocessor.Factory.K_ID, k);
    config.addParameter(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.VARIANTS_ID, 10);
    config.addParameter(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.WINDOW_ID, 1.);
    config.addParameter(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.RANDOM_ID, 0L);
    SpacefillingMaterializeKNNPreprocessor.Factory<DoubleVector> preprocf = ClassGenericsUtil.parameterizeOrAbort(SpacefillingMaterializeKNNPreprocessor.Factory.class, config);
    SpacefillingMaterializeKNNPreprocessor<DoubleVector> preproc = preprocf.instantiate(rel);
    preproc.initialize();
    // add as index
    db.getHierarchy().add(rel, preproc);
    KNNQuery<DoubleVector> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanDistanceKNNQuery);

    // test queries
    testKNNQueries(rel, lin_knn_query, preproc_knn_query, k);
    // also test partial queries, forward only
    testKNNQueries(rel, lin_knn_query, preproc_knn_query, k / 2);
  }

  private void testKNNQueries(Relation<DoubleVector> rep, KNNQuery<DoubleVector> lin_knn_query, KNNQuery<DoubleVector> preproc_knn_query, int k) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(rep.getDBIDs());
    for(DBIDIter it = sample.iter(); it.valid(); it.advance()) {
      KNNList lin_knn = lin_knn_query.getKNNForDBID(it, k);
      KNNList pre_knn = preproc_knn_query.getKNNForDBID(it, k);
      DoubleDBIDListIter lin = lin_knn.iter(), pre = pre_knn.iter();
      for(; lin.valid() && pre.valid(); lin.advance(), pre.advance()) {
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
