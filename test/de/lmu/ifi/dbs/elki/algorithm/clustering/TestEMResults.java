package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.EM;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full EM run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that EM's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestEMResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run EM with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testEMResults() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(EM.SEED_ID, 1);
    params.addParameter(EM.K_ID, 5);
    EM<DoubleVector> em = ClassGenericsUtil.parameterizeOrAbort(EM.class, params);
    testParameterizationOk(params);

    // run EM on database
    Clustering<EMModel<DoubleVector>> result = em.run(db);
    testFMeasure(db, result, 0.961587);
    testClusterSizes(result, new int[] { 5, 91, 98, 200, 316 });
  }
}