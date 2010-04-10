package experimentalcode.elke.algorithm.lof;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Performs a full SUBCLU run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SUBCLU performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * 
 */
public class TestOnlineLOF implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/elki.csv";

  // size of the data set
  int shoulds = 203;

  /**
   * Run SUBCLU with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   * @throws UnableToComplyException 
   */
  @Test
  public void testResults() throws ParameterException, UnableToComplyException {
    int k = 5;
    
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    
    params.addParameter(LOF.K_ID, k);

    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    Integer[] ids = new Integer[] { 1, 98, 99, 45 };
    
    Integer[] insertion_ids = new Integer[]{16,5,7,67,56};
    List<Pair<DoubleVector, DatabaseObjectMetadata>> insertions = new ArrayList<Pair<DoubleVector, DatabaseObjectMetadata>>();
    for (Integer id: insertion_ids) {
      insertions.add(new Pair<DoubleVector, DatabaseObjectMetadata>(db.get(id),new DatabaseObjectMetadata(db, id)));
      db.delete(id);
    }
      
    DistanceFunction<DoubleVector, DoubleDistance> df = new EuclideanDistanceFunction<DoubleVector>();

    System.out.println("\nknn");
    for(int id : ids) {
      List<DistanceResultPair<DoubleDistance>> qr = db.kNNQueryForID(id, k, df);
      System.out.println(qr);
    }

    System.out.println("\nbulk knn");
    List<List<DistanceResultPair<DoubleDistance>>> qrs = db.bulkKNNQueryForID(Arrays.asList(ids), k, df);
    for(List<DistanceResultPair<DoubleDistance>> qrr : qrs)
      System.out.println(qrr);

    List<DistanceResultPair<DoubleDistance>> qr = db.reverseKNNQueryForID(ids[0], k, df);
    System.out.println("\nrnn ");
    System.out.println(qr);

    qrs = db.bulkReverseKNNQueryForID(Arrays.asList(ids), k, df);
    System.out.println("\nbulk rnn ");
    for(List<DistanceResultPair<DoubleDistance>> qrr : qrs)
      System.out.println(qrr);

    // // verify data set size.
    // assertEquals("Database size doesn't match expected size.", shoulds,
    // db.size();
    //
    // setup algorithm
    
    OnlineLOF<DoubleVector, DoubleDistance> lof = new OnlineLOF<DoubleVector, DoubleDistance>(params);
    lof.setVerbose(true);
    
    
     params.failOnErrors();
     if (params.hasUnusedParameters()) {
     fail("Unused parameters: "+params.getRemainingParameters());
     }
     // run LOF on database
     OutlierResult result1 = lof.run(db);
     
     for (Integer id: ids)
       db.insert(insertions);
     
     
     
    //    
    // // run by-label as reference
    // ByLabelHierarchicalClustering<DoubleVector> bylabel = new
    // ByLabelHierarchicalClustering<DoubleVector>();
    // Clustering<Model> rbl = bylabel.run(db);
    //
    // double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    // assertTrue("SUBCLU score on test dataset too low: " + score, score >
    // 0.9090);
    // System.out.println("SUBCLU score: " + score + " > " + 0.9090);
  }
}