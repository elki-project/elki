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
package elki.index;

import static org.junit.Assert.*;

import java.util.Arrays;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.*;
import elki.database.query.ExactPrioritySearcher;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.datasource.filter.FixedDBIDsFilter;
import elki.distance.CosineDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test case to validate some index structures for accuracy. For a known data
 * set and query point, the top 10 nearest neighbors are queried and verified.
 * <p>
 * Note that the internal operation of the index structure is not tested this
 * way, only whether the database object with the index still returns reasonable
 * results.
 * <p>
 * TODO: rather than inheriting from this, simply call the static methods.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractIndexStructureTest {
  // the following values depend on the data set used!
  final static String dataset = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  final static int shoulds = 600;

  // query point
  final static double[] querypoint = new double[] { 0.5, 0.5, 0.5 };

  // coordinates the 10 next neighbors of the query point
  final static double[][] shouldc = new double[][] { //
      { 0.45000428746088883, 0.484504234161508, 0.5538595167151342 }, //
      { 0.4111050036231091, 0.429204794352013, 0.4689430202460606 }, //
      { 0.4758477631164003, 0.6021538103067177, 0.5556807408692025 }, //
      { 0.4163288957164025, 0.49604545242979536, 0.4054361013566713 }, //
      { 0.5819940640461848, 0.48586944418231115, 0.6289592025558619 }, //
      { 0.4373568207802466, 0.3468650110814596, 0.49566951629699485 }, //
      { 0.40283109564192643, 0.6301433694690401, 0.44313571161129883 }, //
      { 0.6545840114867083, 0.4919617658889418, 0.5905461546078652 }, //
      { 0.6011097673869055, 0.6562921241634017, 0.44830647520493694 }, //
      { 0.5127485678175534, 0.29708449200895504, 0.561722374659424 } };

  // and their distances
  final static double[] shouldd = new double[] { //
      0.07510351238126374, 0.11780839322826206, 0.11882371989803064, 0.1263282354232315, //
      0.15347043712184602, 0.1655090505771259, 0.17208323533934652, 0.17933052146586306, //
      0.19319066655063877, 0.21247795391113142 };

  // the 10 next neighbors of the query point
  final static double[][] cosshouldc = new double[][] { //
      { 0.9388918784671444, 0.9369194808634538, 0.9516174288228975 }, //
      { 0.7935657901929466, 0.8267149570739274, 0.8272559426355307 }, //
      { 0.8890643793450695, 0.8437901951504767, 0.8829882896201193 }, //
      { 0.8063843680107571, 0.8398807048463017, 0.7684707062186061 }, //
      { 0.9161117092342763, 0.8422669974489574, 0.8359110785874984 }, //
      { 0.7216963585662849, 0.7890921579518568, 0.8203282873655718 }, //
      { 0.4111050036231091, 0.429204794352013, 0.4689430202460606 }, //
      { 0.9361084305156224, 0.8005811202045534, 0.8467431187531834 }, //
      { 0.769186011075896, 0.7004483428021823, 0.834918908745398 }, //
      { 0.8616135674236818, 0.7527587616292614, 0.9089966965471046 } };

  // and their distances
  final static double[] cosshouldd = new double[] { //
      2.388222990501454E-5, 1.8642729910156586E-4, 2.646439281461799E-4, 6.560940454963804E-4, //
      8.847811747589862E-4, 0.0013998753062922642, 0.0015284394211749763, 0.002127161867922056, //
      0.002544219809804127, 0.003009950345141843 };

  // ids the 10 next neighbors of the query point
  final static int[] shouldc2 = new int[] { //
      1, 40, 130, 72, 116, 170, 41, 60, 159, 193 };

  // and their distances
  final static double[] shouldd2 = new double[] { //
      0.0, 0.056828785196424696, 0.07104616739223207, 0.07666738211759327, //
      0.08788356409628115, 0.08849507874738852, 0.09155789916886654, //
      0.09289490783235752, 0.09533239116740204, 0.09648661162968246 };

  // the 10 next neighbors of the query point
  final static int[] cosshouldc2 = new int[] { //
      1, 130, 72, 7, 40, 191, 170, 41, 57, 60 };

  // and their distances
  final static double[] cosshouldd2 = new double[] { //
      0.0, 0.0013434324976777656, 0.0015118568667449317, 0.0016961681026059772, //
      0.0019224617419847378, 0.001922677208946122, 0.0022000876433134753, //
      0.0022407268615245446, 0.0022821610749839127, 0.00270862943612582 };

  // number of kNN to query
  final static int k = shouldd.length;

  final static double eps = shouldd[shouldd.length - 1];

  final static double eps2 = shouldd2[shouldd2.length - 1];

  final static double coseps = cosshouldd[cosshouldd.length - 1];

  final static double coseps2 = cosshouldd2[cosshouldd2.length - 1];

  /**
   * Verify the neighbors.
   *
   * @param rel Data relation
   * @param dist Distance function
   * @param results Results
   * @param refd Reference distances
   * @param refc Reference vectors
   */
  private static void assertNeighbors(Relation<DoubleVector> rel, DistanceQuery<? super DoubleVector> dist, DoubleDBIDList results, double[] refd, double[][] refc) {
    assertEquals("Result size does not match expectation!", refd.length, results.size());
    for(DoubleDBIDListIter res = results.iter(); res.valid(); res.advance()) {
      int o = res.getOffset();
      assertEquals("Expected distance at offset " + o + " doesn't match.", refd[o], res.doubleValue(), 1e-12);
      double distance = dist.distance(rel.get(res), DoubleVector.wrap(refc[o]));
      assertEquals("Expected vector at offset " + o + " doesn't match: " + rel.get(res).toString(), 0.0, distance, 0.);
    }
  }

  /**
   * Verify the neighbors.
   *
   * @param results Results
   * @param refd Reference distances
   * @param refid Reference ids
   */
  private static void assertNeighbors(DoubleDBIDList results, double[] refd, int[] refid) {
    assertEquals("Result size does not match expectation!", results.size(), refd.length);
    int shift = DBIDUtil.asInteger(results.iter()) - refid[0];
    for(DoubleDBIDListIter res = results.iter(); res.valid(); res.advance()) {
      int o = res.getOffset();
      assertEquals("Expected distance at offset " + o + " doesn't match.", refd[o], res.doubleValue(), 1e-12);
      assertEquals("Expected id at offset " + o + " doesn't match.", refid[o], DBIDUtil.asInteger(res) - shift);
    }
  }

  /**
   * Check the class of a query.
   *
   * @param expect Expected class
   * @param obj observed instance
   * @param obj2 observed other instance
   */
  private static void assertClass(Class<?> expect, Object obj, Object obj2) {
    assertTrue("Expected " + expect + //
        " got: " + (obj != null ? obj.getClass().getName() : "null") + //
        " and " + (obj2 != null ? obj2.getClass().getName() : "null"), //
        expect == null || expect.isInstance(obj) || expect.isInstance(obj2));
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   */
  protected static void assertExactEuclidean(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    assertExactEuclidean(factory, expectKNNQuery, expectRangeQuery, false);
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   * @param dbidonly test DBID queries only
   */
  protected static void assertExactEuclidean(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery, boolean dbidonly) {
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, new FixedDBIDsFilter(0));
    if(factory != null) {
      inputparams.addParameter(StaticArrayDatabase.Par.INDEX_ID, factory);
    }
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    QueryBuilder<DoubleVector> qb = new QueryBuilder<>(relation, EuclideanDistance.STATIC).cheapOnly();
    DistanceQuery<DoubleVector> dist = qb.distanceQuery();
    DBIDRef second = relation.iterDBIDs().advance();

    if(expectKNNQuery != null) {
      KNNSearcher<DoubleVector> knnq = qb.kNNByObject(k);
      KNNSearcher<DBIDRef> knnq2 = qb.kNNByDBID(k);
      assertClass(expectKNNQuery, knnq, knnq2);
      if(!dbidonly) {
        assertNeighbors(relation, dist, knnq.getKNN(DoubleVector.wrap(querypoint), k), shouldd, shouldc);
      }
      assertNeighbors(knnq2.getKNN(second, k), shouldd2, shouldc2);
    }
    if(expectRangeQuery != null) {
      RangeSearcher<DoubleVector> rangeq = qb.rangeByObject(eps);
      RangeSearcher<DBIDRef> rangeq2 = qb.rangeByDBID(eps2);
      assertClass(expectRangeQuery, rangeq, rangeq2);
      if(!dbidonly) {
        assertNeighbors(relation, dist, rangeq.getRange(DoubleVector.wrap(querypoint), eps), shouldd, shouldc);
      }
      assertNeighbors(rangeq2.getRange(second, eps2), shouldd2, shouldc2);
    }
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   */
  protected static void assertExactSqEuclidean(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    assertExactSqEuclidean(factory, expectKNNQuery, expectRangeQuery, false);
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   * @param dbidonly test DBID queries only
   */
  protected static void assertExactSqEuclidean(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery, boolean dbidonly) {
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, new FixedDBIDsFilter(0));
    if(factory != null) {
      inputparams.addParameter(StaticArrayDatabase.Par.INDEX_ID, factory);
    }
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    QueryBuilder<DoubleVector> qb = new QueryBuilder<>(relation, SquaredEuclideanDistance.STATIC).cheapOnly();
    DistanceQuery<DoubleVector> dist = qb.distanceQuery();
    DBIDRef second = relation.iterDBIDs().advance();

    if(expectKNNQuery != null) {
      KNNSearcher<DoubleVector> knnq = qb.kNNByObject(k);
      KNNSearcher<DBIDRef> knnq2 = qb.kNNByDBID(k);
      assertClass(expectKNNQuery, knnq, knnq2);
      if(!dbidonly) {
        assertNeighbors(relation, dist, knnq.getKNN(DoubleVector.wrap(querypoint), k), squared(shouldd), shouldc);
      }
      assertNeighbors(knnq2.getKNN(second, k), squared(shouldd2), shouldc2);
    }
    if(expectRangeQuery != null) {
      RangeSearcher<DoubleVector> rangeq = qb.rangeByObject(eps * eps);
      RangeSearcher<DBIDRef> rangeq2 = qb.rangeByDBID(eps2 * eps2);
      assertClass(expectRangeQuery, rangeq, rangeq2);
      if(!dbidonly) {
        assertNeighbors(relation, dist, rangeq.getRange(DoubleVector.wrap(querypoint), eps * eps), squared(shouldd), shouldc);
      }
      assertNeighbors(rangeq2.getRange(second, eps2 * eps2), squared(shouldd2), shouldc2);
    }
  }

  /**
   * Squared values.
   *
   * @param d
   * @return
   */
  private static double[] squared(double[] d) {
    double[] d2 = new double[d.length];
    for(int i = 0; i < d.length; i++) {
      d2[i] = d[i] * d[i];
    }
    return d2;
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   */
  protected static void assertExactCosine(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    assertExactCosine(factory, expectKNNQuery, expectRangeQuery, false);
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   */
  protected static void assertExactCosine(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery, boolean dbidonly) {
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, new FixedDBIDsFilter(1));
    if(factory != null) {
      inputparams.addParameter(StaticArrayDatabase.Par.INDEX_ID, factory);
    }
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    QueryBuilder<DoubleVector> qb = new QueryBuilder<>(relation, CosineDistance.STATIC).cheapOnly();
    DistanceQuery<DoubleVector> dist = qb.distanceQuery();
    DBIDRef second = relation.iterDBIDs().advance();

    if(expectKNNQuery != null) {
      KNNSearcher<DoubleVector> knnq = qb.cheapOnly().kNNByObject(k);
      KNNSearcher<DBIDRef> knnq2 = qb.cheapOnly().kNNByDBID(k);
      assertClass(expectKNNQuery, knnq, knnq2);
      if(!dbidonly) {
        assertNeighbors(relation, dist, knnq.getKNN(DoubleVector.wrap(querypoint), k), cosshouldd, cosshouldc);
      }
      assertNeighbors(knnq2.getKNN(second, k), cosshouldd2, cosshouldc2);
    }
    if(expectRangeQuery != null) {
      RangeSearcher<DoubleVector> rangeq = qb.cheapOnly().rangeByObject(coseps);
      RangeSearcher<DBIDRef> rangeq2 = qb.cheapOnly().rangeByDBID(coseps);
      assertClass(expectRangeQuery, rangeq, rangeq2);
      if(!dbidonly) {
        assertNeighbors(relation, dist, rangeq.getRange(DoubleVector.wrap(querypoint), coseps), cosshouldd, cosshouldc);
      }
      assertNeighbors(rangeq2.getRange(second, coseps2), cosshouldd2, cosshouldc2);
    }
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectKNNQuery expected knn query class
   * @param expectRangeQuery expected range query class
   */
  protected static void assertSinglePoint(IndexFactory<?> factory, Class<?> expectKNNQuery, Class<?> expectRangeQuery) {
    ArrayAdapterDatabaseConnection dbc = new ArrayAdapterDatabaseConnection(new double[][] { { 1, 0 } });
    Database db = new StaticArrayDatabase(dbc, factory != null ? Arrays.asList(factory) : null);
    db.initialize();
    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    QueryBuilder<DoubleVector> qb = new QueryBuilder<>(relation, EuclideanDistance.STATIC).cheapOnly();
    DBIDRef first = relation.iterDBIDs();
    if(expectKNNQuery != null) {
      KNNSearcher<DBIDRef> knnq = qb.kNNByDBID(1);
      assertClass(expectKNNQuery, knnq, null);
      KNNList knn = knnq.getKNN(first, 1);
      assertEquals("Wrong number of knn results", 1, knn.size());
      assertTrue("Wrong knn result", DBIDUtil.equal(knn.iter(), first));
    }
    if(expectRangeQuery != null) {
      RangeSearcher<DBIDRef> rangeq = qb.rangeByDBID(0.);
      assertClass(expectRangeQuery, rangeq, null);
      DoubleDBIDList range = rangeq.getRange(first, 0);
      assertEquals("Wrong number of range results", 1, range.size());
      assertTrue("Wrong range result", DBIDUtil.equal(range.iter(), first));
    }
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectQuery expected knn query class
   */
  protected static void assertPrioritySearchEuclidean(IndexFactory<?> factory, Class<?> expectQuery) {
    assertPrioritySearchEuclidean(factory, expectQuery, false);
  }

  /**
   * Test helper
   * 
   * @param factory Index factory
   * @param expectQuery expected knn query class
   * @param dbidonly test DBID queries only
   */
  protected static void assertPrioritySearchEuclidean(IndexFactory<?> factory, Class<?> expectQuery, boolean dbidonly) {
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, new FixedDBIDsFilter(1));
    if(factory != null) {
      inputparams.addParameter(StaticArrayDatabase.Par.INDEX_ID, factory);
    }
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds, inputparams);
    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    QueryBuilder<DoubleVector> qb = new QueryBuilder<>(relation, EuclideanDistance.STATIC).cheapOnly();
    DistanceQuery<DoubleVector> dist = qb.distanceQuery();
    DBIDRef second = relation.iterDBIDs().advance();

    PrioritySearcher<DoubleVector> prioq = qb.priorityByObject();
    PrioritySearcher<DBIDRef> prioq2 = qb.priorityByDBID();
    assertClass(expectQuery, prioq, prioq2);
    if(!dbidonly) {
      // get the 10 next neighbors
      DoubleVector dv = DoubleVector.wrap(querypoint);
      { // verify the knn result:
        ModifiableDoubleDBIDList ids = DBIDUtil.newDistanceDBIDList();
        double minall = 0;
        for(prioq.search(dv); prioq.valid(); prioq.advance()) {
          double approx = prioq.getApproximateDistance(); // May be NaN
          double atol = prioq.getApproximateAccuracy(); // May be NaN
          double lb = prioq.getLowerBound(); // May be NaN
          double ub = prioq.getUpperBound(); // May be NaN
          double alllb = prioq.allLowerBound(); // May be NaN
          double exact = prioq.computeExactDistance();
          ids.add(exact, prioq);
          assertFalse("All lower bound decreased", alllb < minall);
          minall = alllb;
          assertFalse("All lower bound incorrect", lb < alllb);
          assertFalse("All lower bound incorrect", exact < alllb * 0.9999999999999999);
          assertFalse("Lower bound incorrect", exact < lb * 0.9999999999999999);
          assertFalse("Upper bound incorrect", exact * 0.9999999999999999 > ub);
          assertFalse("Lower tolerance incorrect", exact < approx - atol);
          assertFalse("Upper tolerance incorrect", exact > approx + atol);
        }
        ids.sort();
        assertTrue("Did not return " + k + " results, but: " + ids.size(), ids.size() >= k);
        assertNeighbors(relation, dist, ids.slice(0, k), shouldd, shouldc);
      }
      assertNeighbors(relation, dist, prioq.getKNN(dv, k), shouldd, shouldc);
      assertNeighbors(relation, dist, prioq.getRange(dv, eps), shouldd, shouldc);
      // Perform a complete, ordered search
      {
        PrioritySearcher<DoubleVector> ex = new ExactPrioritySearcher<>(prioq);
        int c = 0;
        double prevdist = 0;
        for(ex.search(dv); ex.valid(); ex.advance()) {
          assertTrue("Not correctly ordered at " + c, prevdist <= ex.computeExactDistance());
          prevdist = ex.computeExactDistance();
          ++c;
        }
        assertEquals("Incomplete results.", relation.size(), c);
      }
    }
    // get the 10 next neighbors
    { // verify the knn result:
      ModifiableDoubleDBIDList ids = DBIDUtil.newDistanceDBIDList();
      for(prioq2.search(second); prioq2.valid(); prioq2.advance()) {
        double approx = prioq2.getApproximateDistance(); // May be NaN
        double atol = prioq2.getApproximateAccuracy(); // May be NaN
        double lb = prioq2.getLowerBound(); // May be NaN
        double ub = prioq2.getUpperBound(); // May be NaN
        double exact = prioq2.computeExactDistance();
        ids.add(exact, prioq2);
        assertFalse("Lower bound incorrect", exact < lb * 0.9999999999999999);
        assertFalse("Upper bound incorrect", exact * 0.9999999999999999 > ub);
        assertFalse("Lower tolerance incorrect", exact < approx - atol);
        assertFalse("Upper tolerance incorrect", exact > approx + atol);
      }
      ids.sort();
      assertNeighbors(ids.slice(0, k), shouldd2, shouldc2);
    }
    assertNeighbors(prioq2.getKNN(second, k), shouldd2, shouldc2);
    assertNeighbors(prioq2.getRange(second, eps2), shouldd2, shouldc2);
    // Perform a complete, ordered search
    {
      PrioritySearcher<DBIDRef> ex = new ExactPrioritySearcher<>(prioq2);
      int c = 0;
      double prevdist = 0;
      for(ex.search(second); ex.valid(); ex.advance()) {
        assertTrue("Not correctly ordered at " + c, prevdist <= ex.computeExactDistance());
        prevdist = ex.computeExactDistance();
        ++c;
      }
      assertEquals("Incomplete results.", relation.size(), c);
    }
  }
}
