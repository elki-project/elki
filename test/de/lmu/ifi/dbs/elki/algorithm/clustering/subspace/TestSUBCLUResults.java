package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SUBCLU run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SUBCLU performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 */
public class TestSUBCLUResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run SUBCLU with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testSUBCLUResults() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(SUBCLU.EPSILON_ID, 0.001);
    params.addParameter(SUBCLU.MINPTS_ID, 100);

    // setup algorithm
    SUBCLU<DoubleVector> subclu = ClassGenericsUtil.parameterizeOrAbort(SUBCLU.class, params);
    testParameterizationOk(params);

    // run SUBCLU on database
    Clustering<SubspaceModel<DoubleVector>> result = subclu.run(db);

    testFMeasure(db, result, 0.9090);
    testClusterSizes(result, new int[] { 191, 194, 395 });
  }
}