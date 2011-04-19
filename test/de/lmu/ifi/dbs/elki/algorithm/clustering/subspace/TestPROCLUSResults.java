package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full PROCLUS run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that PROCLUS performance
 * doesn't unexpectedly drop on this data set (and also ensures that the
 * algorithms work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestPROCLUSResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run PROCLUS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPROCLUSResults() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(PROCLUS.L_ID, 1);
    params.addParameter(PROCLUS.K_ID, 4);
    params.addParameter(PROCLUS.SEED_ID, 1);

    // setup algorithm
    PROCLUS<DoubleVector> proclus = ClassGenericsUtil.parameterizeOrAbort(PROCLUS.class, params);
    testParameterizationOk(params);

    // run PROCLUS on database
    Clustering<Model> result = proclus.run(db);

    testFMeasure(db, result, 0.68932);
    testClusterSizes(result, new int[] { 78, 93, 203, 226 });
  }

  /**
   * Run PROCLUS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPROCLUSSubspaceOverlapping() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
  
    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(PROCLUS.L_ID, 2);
    params.addParameter(PROCLUS.K_ID, 3);
    params.addParameter(PROCLUS.SEED_ID, 2);
    PROCLUS<DoubleVector> proclus = ClassGenericsUtil.parameterizeOrAbort(PROCLUS.class, params);
    testParameterizationOk(params);
  
    // run PROCLUS on database
    Clustering<Model> result = proclus.run(db);
    testFMeasure(db, result, 0.9673718);
    testClusterSizes(result, new int[] { 150, 289, 411 });
  }
}