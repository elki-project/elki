package de.lmu.ifi.dbs.elki.test.algorithm.clustering.correlation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.FourC;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.test.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Perform a full 4C run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that 4C performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * 
 */
public class TestFourCResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testFourCResults() throws ParameterException {
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>();

    List<String> inputparams = new ArrayList<String>();
    // Set up database input file:
    OptionUtil.addParameter(inputparams, FileBasedDatabaseConnection.INPUT_ID, dataset);
    inputparams = dbconn.setParameters(inputparams);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    FourC<DoubleVector> fourc = new FourC<DoubleVector>();

    // prepare parameters
    ArrayList<String> fourcparams = new ArrayList<String>();
    fourc.setVerbose(false);
    // these parameters are not picked too smartly - 5d in 3d - but it seems to work okay.
    OptionUtil.addParameter(fourcparams, ProjectedDBSCAN.EPSILON_ID, Double.toString(0.30));
    OptionUtil.addParameter(fourcparams, ProjectedDBSCAN.MINPTS_ID, Integer.toString(20));
    OptionUtil.addParameter(fourcparams, ProjectedDBSCAN.LAMBDA_ID, Integer.toString(5));
    
    // Set parameters
    List<String> remainingparams = fourc.setParameters(fourcparams);
    for(String s : remainingparams) {
      System.err.println("Remaining parameter: " + s);
    }
    //System.err.println(fourc.getAttributeSettings().toString());
    assertEquals("Some parameters were ignored by the algorithm.", 0, remainingparams.size());
    // run 4C on database
    Clustering<Model> result = fourc.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    bylabel.run(db);
    Clustering<Model> rbl = bylabel.getResult();

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("FourC score on test dataset too low: " + score, score > 0.79467);
    System.out.println("FourC score: " + score + " > " + 0.79467);
  }

}
