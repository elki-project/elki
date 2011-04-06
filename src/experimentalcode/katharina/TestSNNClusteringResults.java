package experimentalcode.katharina;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.SNNClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SNNClustering run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SNNClustering's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * 
 */
public class TestSNNClusteringResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/katharina/1dbscan_failure.ascii";

  // size of the data set
  int shoulds = 1200;

  /**
   * Run SNNClustering with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testSNNClusteringResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    params.addParameter(FileBasedDatabaseConnection.IDSTART_ID, 1);
    params.addParameter(SNNClustering.EPSILON_ID, "77");
    params.addParameter(SNNClustering.MINPTS_ID, "28");
    params.addParameter(SharedNearestNeighborPreprocessor.Factory.NUMBER_OF_NEIGHBORS_ID, "100");


    FileBasedDatabaseConnection<DoubleVector> dbconn = FileBasedDatabaseConnection.parameterize(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    SNNClustering<DoubleVector, DoubleDistance> snn = ClassGenericsUtil.parameterizeOrAbort(SNNClustering.class, params);

    params.failOnErrors();
    if(params.hasUnusedParameters()) {
      fail("Unused parameters: " + params.getRemainingParameters());
    }
    // run SNN on database
    Clustering<Model> result = snn.run(db);
    List<Cluster<Model>> resultList = result.getAllClusters();
    
    //retrieve and sort cluster sizes of result
    int[] clusterResultSizes = new int[resultList.size()];
    for(int i = 0; i < resultList.size(); i++){
      clusterResultSizes[i] = resultList.get(i).size();
    }  
    java.util.Arrays.sort(clusterResultSizes);

    // run by-label as reference
    ByLabelClustering<DoubleVector> bylabel = new ByLabelClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("SNNClustering score on test dataset too low: " + score, score > 0.83);
    System.out.println("SNNClustering score: " + score + " > " + 0.83);
    int[] expectedClusterSizes = { 76, 213, 219, 225, 231, 236 }; 
    org.junit.Assert.assertArrayEquals("Expected cluster sizes do not match.", expectedClusterSizes, clusterResultSizes);
  }
}