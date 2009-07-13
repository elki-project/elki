package de.lmu.ifi.dbs.elki.test.algorithm.clustering.correlation;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.test.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>();

    List<String> inputparams = new ArrayList<String>();
    // Set up database input file:
    OptionUtil.addParameter(inputparams, FileBasedDatabaseConnection.INPUT_ID, dataset);
    inputparams = dbconn.setParameters(inputparams);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertTrue(db.size() == shoulds);

    // setup algorithm
    COPAC<DoubleVector> copac = new COPAC<DoubleVector>();

    // prepare parameters
    ArrayList<String> copacparams = new ArrayList<String>();
    copac.setVerbose(false);
    // these parameters are not picked too smartly - room for improvement.
    OptionUtil.addParameter(copacparams, COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class.getCanonicalName());
    OptionUtil.addParameter(copacparams, DBSCAN.EPSILON_ID, Double.toString(0.50));
    OptionUtil.addParameter(copacparams, DBSCAN.MINPTS_ID, Integer.toString(30));
    OptionUtil.addParameter(copacparams, COPAC.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getCanonicalName());
    
    // Set parameters
    List<String> remainingparams = copac.setParameters(copacparams);
    for(String s : remainingparams) {
      System.err.println("Remaining parameter: " + s);
    }
    //System.err.println(fourc.getAttributeSettings().toString());
    assertTrue("Some parameters were ignored by the algorithm.", remainingparams.size() == 0);
    // run 4C on database
    Clustering<Model> result = copac.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    bylabel.run(db);
    Clustering<Model> rbl = bylabel.getResult();

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("COPAC score on test dataset too low: " + score, score > 0.66379);
    System.out.println("COPAC score: " + score + " > " + 0.66379);
  }

}
