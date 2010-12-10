package de.lmu.ifi.dbs.elki.index.preprocessed;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.elke.index.preprocessed.MaterializeKNNAndRKNNPreprocessor;

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
  int updatesize = 1;

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
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    Database<DoubleVector> db = dbconn.getDatabase(null);
    DistanceQuery<DoubleVector, DoubleDistance> distanceQuery = db.getDistanceQuery(EuclideanDistanceFunction.STATIC);

    // verify data set size.
    assertTrue(db.size() == shoulds);

    // get linear queries
    LinearScanKNNQuery<DoubleVector, DoubleDistance> lin_knn_query = new LinearScanKNNQuery<DoubleVector, DoubleDistance>(db, distanceQuery);
    LinearScanRKNNQuery<DoubleVector, DoubleDistance> lin_rknn_query = new LinearScanRKNNQuery<DoubleVector, DoubleDistance>(db, distanceQuery, k);

    // get preprocessed queries
    ListParameterization config = new ListParameterization();
    config.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, distanceQuery.getDistanceFunction());
    config.addParameter(MaterializeKNNPreprocessor.K_ID, k);
    MaterializeKNNAndRKNNPreprocessor<DoubleVector, DoubleDistance> preproc = new MaterializeKNNAndRKNNPreprocessor<DoubleVector, DoubleDistance>(config);
    MaterializeKNNAndRKNNPreprocessor.Instance<DoubleVector, DoubleDistance> instance = preproc.instantiate(db);
    KNNQuery<DoubleVector, DoubleDistance> preproc_knn_query = instance.getKNNQuery(db, distanceQuery, k);
    RKNNQuery<DoubleVector, DoubleDistance> preproc_rknn_query = instance.getRKNNQuery(db, distanceQuery);
    // add as index
    db.addIndex(instance);

    // test queries
    testQueries(db, lin_knn_query, lin_rknn_query, preproc_knn_query, preproc_rknn_query);

    // insert new objects
    List<Pair<DoubleVector, DatabaseObjectMetadata>> insertions = new ArrayList<Pair<DoubleVector, DatabaseObjectMetadata>>();
    DoubleVector o = db.get(db.getIDs().iterator().next());
    Random random = new Random(seed);
    for(int i = 0; i < updatesize; i++) {
      DoubleVector obj = o.randomInstance(random);
      insertions.add(new Pair<DoubleVector, DatabaseObjectMetadata>(obj, new DatabaseObjectMetadata()));
    }
    System.out.println("Insert " + insertions);
    System.out.println();
    DBIDs deletions = db.insert(insertions);
    
    // test queries
    testQueries(db, lin_knn_query, lin_rknn_query, preproc_knn_query, preproc_rknn_query);

    // delete objects
    System.out.println("Delete " + deletions);
    db.delete(deletions);

    // test queries
    testQueries(db, lin_knn_query, lin_rknn_query, preproc_knn_query, preproc_rknn_query);

  }

  private void testQueries(Database<DoubleVector> db, KNNQuery<DoubleVector, DoubleDistance> lin_knn_query, RKNNQuery<DoubleVector, DoubleDistance> lin_rknn_query, KNNQuery<DoubleVector, DoubleDistance> preproc_knn_query, RKNNQuery<DoubleVector, DoubleDistance> preproc_rknn_query) {
    ArrayDBIDs sample = DBIDUtil.ensureArray(db.getIDs());
    List<List<DistanceResultPair<DoubleDistance>>> lin_knn_ids = lin_knn_query.getKNNForBulkDBIDs(sample, k);
    List<List<DistanceResultPair<DoubleDistance>>> lin_rknn_ids = lin_rknn_query.getRKNNForBulkDBIDs(sample, k);
    List<List<DistanceResultPair<DoubleDistance>>> preproc_knn_ids = preproc_knn_query.getKNNForBulkDBIDs(sample, k);
    List<List<DistanceResultPair<DoubleDistance>>> preproc_rknn_ids = preproc_rknn_query.getRKNNForBulkDBIDs(sample, k);

    for(int i = 0; i < db.size(); i++) {
      List<DistanceResultPair<DoubleDistance>> lin_knn = lin_knn_ids.get(i);
      List<DistanceResultPair<DoubleDistance>> pre_knn = preproc_knn_ids.get(i);
      if(!lin_knn.equals(pre_knn)) {
        System.out.println("LIN kNN " + lin_knn);
        System.out.println("PRE kNN " + pre_knn);
        System.out.println();
      }
    }
    assertEquals("kNNs of linear scan and preprocessor do not match!", lin_knn_ids, preproc_knn_ids);
    System.out.println("knns ok");

    for(int i = 0; i < db.size(); i++) {
      List<DistanceResultPair<DoubleDistance>> lin_rknn = lin_rknn_ids.get(i);
      List<DistanceResultPair<DoubleDistance>> pre_rknn = preproc_rknn_ids.get(i);
      if(!lin_rknn.equals(pre_rknn)) {
        System.out.println("LIN RkNN " + lin_rknn);
        System.out.println("PRE RkNN " + pre_rknn);
        System.out.println();
      }
    }
    assertEquals("RkNNs of linear scan and preprocessor do not match!", lin_rknn_ids, preproc_rknn_ids);
    System.out.println("rknns ok");
    System.out.println();
  }

}