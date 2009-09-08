package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>();

    List<String> inputparams = new ArrayList<String>();
    // Set up database input file:
    OptionUtil.addParameter(inputparams, FileBasedDatabaseConnection.INPUT_ID, dataset);
    OptionUtil.addParameter(inputparams, FileBasedDatabaseConnection.CLASS_LABEL_INDEX_ID, "1");
    inputparams = dbconn.setParameters(inputparams);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    PreDeCon<DoubleVector> predecon = new PreDeCon<DoubleVector>();

    // prepare parameters
    ArrayList<String> predeconparams = new ArrayList<String>();
    predecon.setVerbose(false);
    // these parameters are not picked too smartly - room for improvement.
    OptionUtil.addParameter(predeconparams, ProjectedDBSCAN.EPSILON_ID, Double.toString(50));
    OptionUtil.addParameter(predeconparams, ProjectedDBSCAN.MINPTS_ID, Integer.toString(50));
    OptionUtil.addParameter(predeconparams, ProjectedDBSCAN.LAMBDA_ID, Integer.toString(5));
    
    // Set parameters
    List<String> remainingparams = predecon.setParameters(predeconparams);
    for(String s : remainingparams) {
      System.err.println("Remaining parameter: " + s);
    }
    //System.err.println(fourc.getAttributeSettings().toString());
    assertEquals("Some parameters were ignored by the algorithm.", 0, remainingparams.size());
    // run 4C on database
    Clustering<Model> result = predecon.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    bylabel.run(db);
    Clustering<Model> rbl = bylabel.getResult();

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("PreDeCon score on test dataset too low: " + score, score > 0.520489);
    System.out.println("PreDeCon score: " + score + " > " + 0.520489);
  }

}
