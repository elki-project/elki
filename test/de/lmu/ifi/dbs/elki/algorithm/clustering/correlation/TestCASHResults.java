package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.parser.ParameterizationFunctionLabelParser;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Perform a full CASH run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that CASH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * 
 */
public class TestCASHResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testERiCResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    // Input
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    params.addParameter(FileBasedDatabaseConnection.IDSTART_ID, 1);
    // CASH input
    params.addParameter(FileBasedDatabaseConnection.PARSER_ID, ParameterizationFunctionLabelParser.class);
    // CASH parameters
    params.addParameter(CASH.JITTER_ID, 0.7);
    params.addParameter(CASH.MINPTS_ID, 50);
    params.addParameter(CASH.MAXLEVEL_ID, 25);
    params.addFlag(CASH.ADJUST_ID);
    
    FileBasedDatabaseConnection<ParameterizationFunction> dbconn = FileBasedDatabaseConnection.parameterize(params);
    // get database
    Database<ParameterizationFunction> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    CASH cash = new CASH(params);
    
    params.failOnErrors();
    if (params.hasUnusedParameters()) {
      for (Pair<OptionID,Object> pair : params.getRemainingParameters()) {
        LoggingUtil.warning("Unused: " + pair.first.getName() + " : " + pair.second.toString());
      }
      fail("Unused parameters.");
    }
    // run ERiC on database
    Clustering<Model> result = cash.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<ParameterizationFunction> bylabel = new ByLabelHierarchicalClustering<ParameterizationFunction>();
    Clustering<Model> rbl = bylabel.run(db);

    // Even with not optimized parameters, we easily achieved 0.62
    // So any loss of quality means something isn't quite right with our
    // algorithms.
    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("CASH score on test dataset too low: " + score, score > 0.638);
    System.out.println("CASH score: " + score + " > " + 0.638);
  }
}