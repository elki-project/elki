package experimentalcode.elke.algorithm.lof;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SUBCLU run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SUBCLU performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * 
 */
public class TestElkilein implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/elki.csv";

  // size of the data set
  int shoulds = 203;

  /**
   * Run SUBCLU with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);
    
    Integer[] ids = new Integer[] {1,98,99,45};
    int k = 5;
    
    DistanceFunction<DoubleVector, DoubleDistance> df = new EuclideanDistanceFunction<DoubleVector>();
    
    System.out.println("\nknn");
    for (int id: ids) {
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
    // // setup algorithm
    // SUBCLU<DoubleVector, DoubleDistance> subclu = new SUBCLU<DoubleVector,
    // DoubleDistance>(params);
    // subclu.setVerbose(false);
    //
    // params.failOnErrors();
    // if (params.hasUnusedParameters()) {
    // fail("Unused parameters: "+params.getRemainingParameters());
    // }
    // // run SUBCLU on database
    // Clustering<SubspaceModel<DoubleVector>> result = subclu.run(db);
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