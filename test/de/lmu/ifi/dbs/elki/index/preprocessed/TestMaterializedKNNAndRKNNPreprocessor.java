package de.lmu.ifi.dbs.elki.index.preprocessed;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
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
    FileBasedDatabaseConnection dbconn = ClassGenericsUtil.parameterizeOrAbort(FileBasedDatabaseConnection.class, params);
    Database db = dbconn.getDatabase();
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector, DoubleDistance> distanceQuery = db.getDistanceQuery(rep, EuclideanDistanceFunction.STATIC);

    // verify data set size.
    assertEquals("Data set size doesn't match parameters.", shoulds, rep.size());

    // get linear queries
    LinearScanKNNQuery<DoubleVector, DoubleDistance> lin_knn_query = new LinearScanKNNQuery<DoubleVector, DoubleDistance>(rep, distanceQuery);
    LinearScanRKNNQuery<DoubleVector, DoubleDistance> lin_rknn_query = new LinearScanRKNNQuery<DoubleVector, DoubleDistance>(rep, distanceQuery, lin_knn_query, k);

    // get preprocessed queries
    ListParameterization config = new ListParameterization();
    config.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, distanceQuery.getDistanceFunction());
    config.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, k);
    MaterializeKNNAndRKNNPreprocessor<DoubleVector, DoubleDistance> preproc = new MaterializeKNNAndRKNNPreprocessor<DoubleVector, DoubleDistance>(rep, distanceQuery.getDistanceFunction(), k);
    KNNQuery<DoubleVector, DoubleDistance> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    RKNNQuery<DoubleVector, DoubleDistance> preproc_rknn_query = preproc.getRKNNQuery(distanceQuery);
    // add as index
    db.addIndex(preproc);

    // test queries
    testQueries(rep, lin_knn_query, lin_rknn_query, preproc_knn_query, preproc_rknn_query);

    // insert new objects
    List<DoubleVector> insertions = new ArrayList<DoubleVector>();
    DoubleVector o = DatabaseUtil.assumeVectorField(rep).getFactory();
    Random random = new Random(seed);
    for(int i = 0; i < updatesize; i++) {
      DoubleVector obj = o.randomInstance(random);
      insertions.add(obj);
    }
    System.out.println("Insert " + insertions);
    System.out.println();
    DBIDs deletions = db.insert(MultipleObjectsBundle.makeSimple(rep.getDataTypeInformation(), insertions));

    // test queries
    testQueries(rep, lin_knn_query, lin_rknn_query, preproc_knn_query, preproc_rknn_query);

    // delete objects
    System.out.println("Delete " + deletions);
    db.delete(deletions);

    // test queries
    testQueries(rep, lin_knn_query, lin_rknn_query, preproc_knn_query, preproc_rknn_query);

  }

  private void testQueries(Relation<DoubleVector> rep, KNNQuery<DoubleVector, DoubleDistance> lin_knn_query, RKNNQuery<DoubleVector, DoubleDistance> lin_rknn_query, KNNQuery<DoubleVector, DoubleDistance> preproc_knn_query, RKNNQuery<DoubleVector, DoubleDistance> preproc_rknn_query) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(rep.getDBIDs());
    List<List<DistanceResultPair<DoubleDistance>>> lin_knn_ids = lin_knn_query.getKNNForBulkDBIDs(sample, k);
    List<List<DistanceResultPair<DoubleDistance>>> lin_rknn_ids = lin_rknn_query.getRKNNForBulkDBIDs(sample, k);
    List<List<DistanceResultPair<DoubleDistance>>> preproc_knn_ids = preproc_knn_query.getKNNForBulkDBIDs(sample, k);
    List<List<DistanceResultPair<DoubleDistance>>> preproc_rknn_ids = preproc_rknn_query.getRKNNForBulkDBIDs(sample, k);

    for(int i = 0; i < rep.size(); i++) {
      List<DistanceResultPair<DoubleDistance>> lin_knn = lin_knn_ids.get(i);
      List<DistanceResultPair<DoubleDistance>> pre_knn = preproc_knn_ids.get(i);
      if(!lin_knn.equals(pre_knn)) {
        System.out.println("LIN kNN " + lin_knn);
        System.out.println("PRE kNN " + pre_knn);
      }
      assertEquals("kNN sizes do not agree.", lin_knn.size(), pre_knn.size());
      for(int j = 0; j < lin_knn.size(); j++) {
        assertTrue("kNNs of linear scan and preprocessor do not match!", lin_knn.get(j).getDBID().equals(pre_knn.get(j).getDBID()));
        assertTrue("kNNs of linear scan and preprocessor do not match!", lin_knn.get(j).getDistance().equals(pre_knn.get(j).getDistance()));
      }
    }
    System.out.println("knns ok");

    for(int i = 0; i < rep.size(); i++) {
      List<DistanceResultPair<DoubleDistance>> lin_rknn = lin_rknn_ids.get(i);
      List<DistanceResultPair<DoubleDistance>> pre_rknn = preproc_rknn_ids.get(i);
      if(!lin_rknn.equals(pre_rknn)) {
        System.out.println("LIN RkNN " + lin_rknn);
        System.out.println("PRE RkNN " + pre_rknn);
        System.out.println();
      }
      assertEquals("rkNN sizes do not agree.", lin_rknn.size(), pre_rknn.size());
      for(int j = 0; j < lin_rknn.size(); j++) {
        assertTrue("rkNNs of linear scan and preprocessor do not match!", lin_rknn.get(j).getDBID().equals(pre_rknn.get(j).getDBID()));
        assertTrue("rkNNs of linear scan and preprocessor do not match!", lin_rknn.get(j).getDistance().equals(pre_rknn.get(j).getDistance()));
      }
    }
    System.out.println("rknns ok");
    System.out.println();
  }

}