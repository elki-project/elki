package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the GaussianModel algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestGaussianModel extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testGaussianModel() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();

    // setup Algorithm
    GaussianModel<DoubleVector> gaussianModel = ClassGenericsUtil.parameterizeOrAbort(GaussianModel.class, params);
    testParameterizationOk(params);

    // run GaussianModel on database
    OutlierResult result = gaussianModel.run(db);

    testSingleScore(result, 1025, 2.8312466458765426);
    testAUC(db, "Noise", result, 0.9937641025641025);
  }
}