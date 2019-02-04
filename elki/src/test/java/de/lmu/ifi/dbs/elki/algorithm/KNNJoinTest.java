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
package de.lmu.ifi.dbs.elki.algorithm;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for kNN joins.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class KNNJoinTest {
  // the following values depend on the data set used!
  String dataset = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  // size of the data set
  int shoulds = 20;

  // mean number of 2NN
  double mean2nnEuclid = 2.85;

  // variance
  double var2nnEuclid = 0.87105;

  // mean number of 2NN
  double mean2nnManhattan = 2.9;

  // variance
  double var2nnManhattan = 0.83157894;

  @Test
  public void testLinearScan() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds);
    Relation<NumberVector> relation = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    // Euclidean
    {
      DistanceQuery<NumberVector> dq = db.getDistanceQuery(relation, EuclideanDistanceFunction.STATIC);
      KNNQuery<NumberVector> knnq = QueryUtil.getLinearScanKNNQuery(dq);

      MeanVariance meansize = new MeanVariance();
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        meansize.put(knnq.getKNNForDBID(iditer, 2).size());
      }
      org.junit.Assert.assertEquals("Euclidean mean 2NN", mean2nnEuclid, meansize.getMean(), 0.00001);
      org.junit.Assert.assertEquals("Euclidean variance 2NN", var2nnEuclid, meansize.getSampleVariance(), 0.00001);
    }
    // Manhattan
    {
      DistanceQuery<NumberVector> dq = db.getDistanceQuery(relation, ManhattanDistanceFunction.STATIC);
      KNNQuery<NumberVector> knnq = QueryUtil.getLinearScanKNNQuery(dq);

      MeanVariance meansize = new MeanVariance();
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        meansize.put(knnq.getKNNForDBID(iditer, 2).size());
      }
      org.junit.Assert.assertEquals("Manhattan mean 2NN", mean2nnManhattan, meansize.getMean(), 0.00001);
      org.junit.Assert.assertEquals("Manhattan variance 2NN", var2nnManhattan, meansize.getSampleVariance(), 0.00001);
    }
  }

  /**
   * Test {@link RStarTree} using a file based database connection.
   */
  @Test
  public void testKNNJoinRtreeMini() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 200);
    doKNNJoin(spatparams);
  }

  /**
   * Test {@link RStarTree} using a file based database connection.
   */
  @Test
  public void testKNNJoinRtreeMaxi() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 2000);
    doKNNJoin(spatparams);
  }

  /**
   * Test {@link DeLiCluTree} using a file based database connection.
   */
  @Test
  public void testKNNJoinDeLiCluTreeMini() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, DeLiCluTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 200);
    doKNNJoin(spatparams);
  }

  /**
   * Actual test routine.
   *
   * @param inputparams
   */
  void doKNNJoin(ListParameterization inputparams) {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<NumberVector> relation = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    // Euclidean
    {
      KNNJoin<DoubleVector, ?, ?> knnjoin = new KNNJoin<DoubleVector, RStarTreeNode, SpatialEntry>(EuclideanDistanceFunction.STATIC, 2);
      Relation<KNNList> result = knnjoin.run(db);

      MeanVariance meansize = new MeanVariance();
      for(DBIDIter id = relation.getDBIDs().iter(); id.valid(); id.advance()) {
        meansize.put(result.get(id).size());
      }
      org.junit.Assert.assertEquals("Euclidean mean 2NN set size", mean2nnEuclid, meansize.getMean(), 0.00001);
      org.junit.Assert.assertEquals("Euclidean variance 2NN", var2nnEuclid, meansize.getSampleVariance(), 0.00001);
    }
    // Manhattan
    {
      KNNJoin<DoubleVector, ?, ?> knnjoin = new KNNJoin<DoubleVector, RStarTreeNode, SpatialEntry>(ManhattanDistanceFunction.STATIC, 2);
      Relation<KNNList> result = knnjoin.run(db);

      MeanVariance meansize = new MeanVariance();
      for(DBIDIter id = relation.getDBIDs().iter(); id.valid(); id.advance()) {
        meansize.put(result.get(id).size());
      }
      org.junit.Assert.assertEquals("Manhattan mean 2NN", mean2nnManhattan, meansize.getMean(), 0.00001);
      org.junit.Assert.assertEquals("Manhattan variance 2NN", var2nnManhattan, meansize.getSampleVariance(), 0.00001);
    }
  }
}
