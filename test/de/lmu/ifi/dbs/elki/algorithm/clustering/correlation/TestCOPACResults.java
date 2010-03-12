package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full COPAC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that COPAC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * 
 */
public class TestCOPACResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Run COPAC with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testCOPACResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    // these parameters are not picked too smartly - room for improvement.
    params.addParameter(COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class);
    params.addParameter(DBSCAN.EPSILON_ID, "0.50");
    params.addParameter(DBSCAN.MINPTS_ID, 30);
    params.addParameter(COPAC.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class);
    
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    COPAC<DoubleVector> copac = new COPAC<DoubleVector>(params);
    copac.setVerbose(false);

    params.failOnErrors();
    if (params.hasUnusedParameters()) {
      fail("Unused parameters: "+params.getRemainingParameters());
    }
    // run 4C on database
    Clustering<Model> result = copac.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("COPAC score on test dataset too low: " + score, score > 0.66379);
    System.out.println("COPAC score: " + score + " > " + 0.66379);
  }
}