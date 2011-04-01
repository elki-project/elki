package experimentalcode.katharina;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full OPTICS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that OPTICS's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * 
 */
public class TestOPTICSResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "src/experimentalcode/katharina/katharina/generiert/1hierarchischesCluster.csv";

  // size of the data set
  int shoulds = 710;

  /**
   * Run OPTICS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDeLiCluResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    params.addParameter(FileBasedDatabaseConnection.IDSTART_ID, 1);
    params.addParameter(OPTICS.XI_ID, "0.038");
    params.addParameter(OPTICS.MINPTS_ID, 18);

    FileBasedDatabaseConnection<DoubleVector> dbconn = FileBasedDatabaseConnection.parameterize(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    OPTICS<DoubleVector, DoubleDistance> optics = ClassGenericsUtil.parameterizeOrAbort(OPTICS.class, params);

    params.failOnErrors();
    if(params.hasUnusedParameters()) {
      fail("Unused parameters: " + params.getRemainingParameters());
    }
    // run OPTICS on database
    //TODO: type mismatch for result
    ClusterOrderResult<DoubleDistance> result = optics.run(db);
    //TODO: parameters need to become dynamic
    OPTICSXi.extractClusters(result, db, 0.038, 18);

    // run by-label as reference
    ByLabelClustering<DoubleVector> bylabel = new ByLabelClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("OPTICS score on test dataset too low: " + score, score > 0.950);
    System.out.println("OPTICS score: " + score + " > " + 0.950);
  }
}