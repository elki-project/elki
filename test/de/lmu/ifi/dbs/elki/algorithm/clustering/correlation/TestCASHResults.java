package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.parser.ParameterizationFunctionLabelParser;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full CASH run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that CASH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 */
public class TestCASHResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run CASH with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCASHResults() throws ParameterException {
    ListParameterization inp = new ListParameterization();
    // CASH input
    inp.addParameter(FileBasedDatabaseConnection.PARSER_ID, ParameterizationFunctionLabelParser.class);
    // Input
    Database<ParameterizationFunction> db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600, inp);
    
    // CASH parameters
    ListParameterization params = new ListParameterization();
    params.addParameter(CASH.JITTER_ID, 0.7);
    params.addParameter(CASH.MINPTS_ID, 50);
    params.addParameter(CASH.MAXLEVEL_ID, 25);
    params.addFlag(CASH.ADJUST_ID);

    // setup algorithm
    CASH cash = ClassGenericsUtil.parameterizeOrAbort(CASH.class, params);
    testParameterizationOk(params);

    // run CASH on database
    Clustering<Model> result = cash.run(db);

    testFMeasureHierarchical(db, result, 0.638727);
    testClusterSizes(result, new int[] { 13, 65, 74, 75, 442 });
  }
}