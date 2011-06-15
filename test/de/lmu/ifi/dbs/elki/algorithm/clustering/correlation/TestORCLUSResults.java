package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

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
 * Performs a full ORCLUS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that ORCLUS performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 */
public class TestORCLUSResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run ORCLUS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testORCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-hierarchy.csv", 450);

    ListParameterization params = new ListParameterization();
    // these parameters are not picked too smartly - room for improvement.
    params.addParameter(ORCLUS.K_ID, 3);
    params.addParameter(ORCLUS.L_ID, 1);
    params.addParameter(ORCLUS.SEED_ID, 2);

    // setup algorithm
    ORCLUS<DoubleVector> orclus = ClassGenericsUtil.parameterizeOrAbort(ORCLUS.class, params);
    testParameterizationOk(params);

    // run ORCLUS on database
    Clustering<Model> result = orclus.run(db);

    testFMeasureHierarchical(db, result, 0.789113);
    testClusterSizes(result, new int[] { 22, 27, 401 });
  }

  /**
   * Run ORCLUS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testORCLUSSkewedDisjoint() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-skewed-disjoint-3-5d.ascii", 601);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(ORCLUS.K_ID, 3);
    params.addParameter(ORCLUS.L_ID, 4);
    params.addParameter(ORCLUS.SEED_ID, 9);

    ORCLUS<DoubleVector> orclus = ClassGenericsUtil.parameterizeOrAbort(ORCLUS.class, params);
    testParameterizationOk(params);

    // run ORCLUS on database
    Clustering<Model> result = orclus.run(db);
    testFMeasure(db, result, 0.8687866);
    testClusterSizes(result, new int[] { 170, 200, 231 });
  }
}