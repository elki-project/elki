package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PreDeCon;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full PreDeCon run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that PreDeCon performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * 
 */
public class TestPreDeConResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/axis-parallel-subspace-clusters-6d.csv.gz";

  // size of the data set
  int shoulds = 2500;

  /**
   * Run PreDeCon with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPreDeConResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    // Input
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    params.addParameter(FileBasedDatabaseConnection.CLASS_LABEL_INDEX_ID, 1);
    // PreDeCon
    // these parameters are not picked too smartly - room for improvement.
    params.addParameter(ProjectedDBSCAN.EPSILON_ID, "50");
    params.addParameter(ProjectedDBSCAN.MINPTS_ID, 50);
    params.addParameter(ProjectedDBSCAN.LAMBDA_ID, 5);
    
    
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    PreDeCon<DoubleVector> predecon = new PreDeCon<DoubleVector>(params);
    predecon.setVerbose(false);
    
    params.failOnErrors();
    if (params.hasUnusedParameters()) {
      fail("Unused parameters: "+params.getRemainingParameters());
    }
    
    // run PredeCon on database
    Clustering<Model> result = predecon.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("PreDeCon score on test dataset too low: " + score, score > 0.520489);
    System.out.println("PreDeCon score: " + score + " > " + 0.520489);
  }
}