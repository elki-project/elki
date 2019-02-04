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
package de.lmu.ifi.dbs.elki.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.FixedDBIDsFilter;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test case to validate some index structures for accuracy. For a known data
 * set and query point, the top 10 nearest neighbors are queried and verified.
 *
 * Note that the internal operation of the index structure is not tested this
 * way, only whether the database object with the index still returns reasonable
 * results.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractIndexStructureTest {
  // the following values depend on the data set used!
  String dataset = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  // query point
  double[] querypoint = new double[] { 0.5, 0.5, 0.5 };

  // number of kNN to query
  int k = 10;

  // the 10 next neighbors of the query point
  double[][] shouldc = new double[][] { //
      { 0.45000428746088883, 0.484504234161508, 0.5538595167151342 }, //
      { 0.4111050036231091, 0.429204794352013, 0.4689430202460606 }, //
      { 0.4758477631164003, 0.6021538103067177, 0.5556807408692025 }, //
      { 0.4163288957164025, 0.49604545242979536, 0.4054361013566713 }, //
      { 0.5819940640461848, 0.48586944418231115, 0.6289592025558619 }, //
      { 0.4373568207802466, 0.3468650110814596, 0.49566951629699485 }, //
      { 0.40283109564192643, 0.6301433694690401, 0.44313571161129883 }, //
      { 0.6545840114867083, 0.4919617658889418, 0.5905461546078652 }, //
      { 0.6011097673869055, 0.6562921241634017, 0.44830647520493694 }, //
      { 0.5127485678175534, 0.29708449200895504, 0.561722374659424 }, //
  };

  // and their distances
  double[] shouldd = new double[] { //
      0.07510351238126374, 0.11780839322826206, 0.11882371989803064, 0.1263282354232315, //
      0.15347043712184602, 0.1655090505771259, 0.17208323533934652, 0.17933052146586306, //
      0.19319066655063877, 0.21247795391113142 };

  // the 10 next neighbors of the query point
  double[][] cosshouldc = new double[][] { //
      { 0.9388918784671444, 0.9369194808634538, 0.9516174288228975 }, //
      { 0.7935657901929466, 0.8267149570739274, 0.8272559426355307 }, //
      { 0.8890643793450695, 0.8437901951504767, 0.8829882896201193 }, //
      { 0.8063843680107571, 0.8398807048463017, 0.7684707062186061 }, //
      { 0.9161117092342763, 0.8422669974489574, 0.8359110785874984 }, //
      { 0.7216963585662849, 0.7890921579518568, 0.8203282873655718 }, //
      { 0.4111050036231091, 0.429204794352013, 0.4689430202460606 }, //
      { 0.9361084305156224, 0.8005811202045534, 0.8467431187531834 }, //
      { 0.769186011075896, 0.7004483428021823, 0.834918908745398 }, //
      { 0.8616135674236818, 0.7527587616292614, 0.9089966965471046 }, //
  };

  // and their distances
  double[] cosshouldd = new double[] { //
      2.388222990501454E-5, 1.8642729910156586E-4, 2.646439281461799E-4, 6.560940454963804E-4, //
      8.847811747589862E-4, 0.0013998753062922642, 0.0015284394211749763, 0.002127161867922056, //
      0.002544219809804127, 0.003009950345141843 };

  double eps = shouldd[shouldd.length - 1];

  double coseps = cosshouldd[cosshouldd.length - 1];

  /**
   * Actual test routine.
   *
   * @param inputparams
   */
  protected void testExactEuclidean(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, new FixedDBIDsFilter(1));
    if(factory != null) {
      inputparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, factory);
    }
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> dist = db.getDistanceQuery(rep, EuclideanDistanceFunction.STATIC);

    if(expectKNNQuery != null) {
      // get the 10 next neighbors
      DoubleVector dv = DoubleVector.wrap(querypoint);
      KNNQuery<DoubleVector> knnq = db.getKNNQuery(dist, k);
      assertTrue("Returned knn query is not of expected class: expected " + expectKNNQuery + " got " + knnq.getClass(), expectKNNQuery.isAssignableFrom(knnq.getClass()));
      KNNList ids = knnq.getKNNForObject(dv, k);
      assertEquals("Result size does not match expectation!", shouldd.length, ids.size(), 1e-15);

      // verify that the neighbors match.
      int i = 0;
      for(DoubleDBIDListIter res = ids.iter(); res.valid(); res.advance(), i++) {
        // Verify distance
        assertEquals("Expected distance doesn't match.", shouldd[i], res.doubleValue(), 1e-6);
        // verify vector
        DoubleVector c = rep.get(res);
        DoubleVector c2 = DoubleVector.wrap(shouldc[i]);
        assertEquals("Expected vector doesn't match: " + c.toString(), 0.0, dist.distance(c, c2), 1e-15);
      }
    }
    if(expectRangeQuery != null) {
      // Do a range query
      DoubleVector dv = DoubleVector.wrap(querypoint);
      RangeQuery<DoubleVector> rangeq = db.getRangeQuery(dist, eps);
      assertTrue("Returned range query is not of expected class: expected " + expectRangeQuery + " got " + rangeq.getClass(), expectRangeQuery.isAssignableFrom(rangeq.getClass()));
      DoubleDBIDList ids = rangeq.getRangeForObject(dv, eps);
      assertEquals("Result size does not match expectation!", shouldd.length, ids.size(), 1e-15);

      // verify that the neighbors match.
      int i = 0;
      for(DoubleDBIDListIter res = ids.iter(); res.valid(); res.advance(), i++) {
        // Verify distance
        assertEquals("Expected distance doesn't match.", shouldd[i], res.doubleValue(), 1e-6);
        // verify vector
        DoubleVector c = rep.get(res);
        DoubleVector c2 = DoubleVector.wrap(shouldc[i]);
        assertEquals("Expected vector doesn't match: " + c.toString(), 0.0, dist.distance(c, c2), 1e-15);
      }
    }
  }

  /**
   * Actual test routine, for cosine distance
   *
   * @param inputparams
   */
  protected void testExactCosine(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, new FixedDBIDsFilter(1));
    if(factory != null) {
      inputparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, factory);
    }
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> dist = db.getDistanceQuery(rep, CosineDistanceFunction.STATIC);

    if(expectKNNQuery != null) {
      // get the 10 next neighbors
      DoubleVector dv = DoubleVector.wrap(querypoint);
      KNNQuery<DoubleVector> knnq = db.getKNNQuery(dist, k);
      assertTrue("Returned knn query is not of expected class: expected " + expectKNNQuery + " got " + knnq.getClass(), expectKNNQuery.isAssignableFrom(knnq.getClass()));
      KNNList ids = knnq.getKNNForObject(dv, k);
      assertEquals("Result size does not match expectation!", cosshouldd.length, ids.size());

      // verify that the neighbors match.
      int i = 0;
      for(DoubleDBIDListIter res = ids.iter(); res.valid(); res.advance(), i++) {
        // Verify distance
        assertEquals("Expected distance doesn't match.", cosshouldd[i], res.doubleValue(), 1e-15);
        // verify vector
        DoubleVector c = rep.get(res);
        DoubleVector c2 = DoubleVector.wrap(cosshouldc[i]);
        assertEquals("Expected vector doesn't match: " + c.toString(), 0.0, dist.distance(c, c2), 1e-15);
      }
    }
    if(expectRangeQuery != null) {
      // Do a range query
      DoubleVector dv = DoubleVector.wrap(querypoint);
      RangeQuery<DoubleVector> rangeq = db.getRangeQuery(dist, coseps);
      assertTrue("Returned range query is not of expected class: expected " + expectRangeQuery + " got " + rangeq.getClass(), expectRangeQuery.isAssignableFrom(rangeq.getClass()));
      DoubleDBIDList ids = rangeq.getRangeForObject(dv, coseps);
      assertEquals("Result size does not match expectation!", cosshouldd.length, ids.size());

      // verify that the neighbors match.
      int i = 0;
      for(DoubleDBIDListIter res = ids.iter(); res.valid(); res.advance(), i++) {
        // Verify distance
        assertEquals("Expected distance doesn't match.", cosshouldd[i], res.doubleValue(), 1e-15);
        // verify vector
        DoubleVector c = rep.get(res);
        DoubleVector c2 = DoubleVector.wrap(cosshouldc[i]);
        assertEquals("Expected vector doesn't match: " + c.toString(), 0.0, dist.distance(c, c2), 1e-15);
      }
    }
  }

  /**
   * Test degenerate case: single point.
   * 
   * @param factory
   */
  protected void testSinglePoint(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    ArrayAdapterDatabaseConnection dbc = new ArrayAdapterDatabaseConnection(new double[][] { { 1, 0 } });
    Database db = new StaticArrayDatabase(dbc, factory != null ? Arrays.asList(factory) : null);
    db.initialize();
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> dist = db.getDistanceQuery(rep, EuclideanDistanceFunction.STATIC);
    DBIDRef first = rep.iterDBIDs();
    if(expectKNNQuery != null) {
      KNNQuery<DoubleVector> knnq = rep.getKNNQuery(dist);
      assertTrue("Returned knn query is not of expected class: expected " + expectKNNQuery + " got " + knnq.getClass(), expectKNNQuery.isAssignableFrom(knnq.getClass()));
      KNNList knn = knnq.getKNNForDBID(first, 1);
      assertEquals("Wrong number of knn results", 1, knn.size());
      assertTrue("Wrong knn result", DBIDUtil.equal(knn.iter(), first));
    }
    if(expectRangeQuery != null) {
      RangeQuery<DoubleVector> rangeq = rep.getRangeQuery(dist);
      assertTrue("Returned range query is not of expected class: expected " + expectRangeQuery + " got " + rangeq.getClass(), expectRangeQuery.isAssignableFrom(rangeq.getClass()));
      DoubleDBIDList range = rangeq.getRangeForDBID(first, 0);
      assertEquals("Wrong number of range results", 1, range.size());
      assertTrue("Wrong range result", DBIDUtil.equal(range.iter(), first));
    }
  }
}
