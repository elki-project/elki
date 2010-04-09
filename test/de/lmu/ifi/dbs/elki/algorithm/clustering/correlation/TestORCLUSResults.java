package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full ORCLUS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that ORCLUS performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * 
 */
public class TestORCLUSResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/correlation-hierarchy.csv";

  // size of the data set
  int shoulds = 450;

  /**
   * Run ORCLUS with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testORCLUSResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    // these parameters are not picked too smartly - room for improvement.
    params.addParameter(ORCLUS.K_ID, 3);
    params.addParameter(ORCLUS.L_ID, 1);
    
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    ORCLUS<DoubleVector> orclus = new ORCLUS<DoubleVector>(params);
    orclus.setVerbose(false);

    params.failOnErrors();
    if (params.hasUnusedParameters()) {
      fail("Unused parameters: "+params.getRemainingParameters());
    }
    // run ORCLUS on database
    Clustering<Model> result = orclus.run(db);
    
    // run by-label as reference
    ByLabelClustering<DoubleVector> bylabel = new ByLabelClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("ORCLUS score on test dataset too low: " + score, score > 0.6345);
    System.out.println("ORCLUS score: " + score + " > " + 0.6345);
  }
}