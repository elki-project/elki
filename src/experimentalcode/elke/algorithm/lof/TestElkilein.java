package experimentalcode.elke.algorithm.lof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.SUBCLU;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
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
    
    int id1 = 1;
    int id2 = 45;
    
    List<DistanceResultPair<DoubleDistance>> qr = db.kNNQueryForID(id1, 7, new EuclideanDistanceFunction<DoubleVector>());
    System.out.println(qr);
    qr = db.kNNQueryForID(id2, 7, new EuclideanDistanceFunction<DoubleVector>());
    System.out.println(qr);
    
    System.out.println();

    List<Integer> ids = new ArrayList<Integer>();
    ids.add(id1);
    ids.add(id2);
    List<List<DistanceResultPair<DoubleDistance>>> qrs = db.bulkKNNQueryForID(ids, 7, new EuclideanDistanceFunction<DoubleVector>());
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