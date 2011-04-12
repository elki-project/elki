package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.GaussianUniformMixture;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the GaussianUniformMixture algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestGaussianUniformMixture extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testGaussianUniformMixture() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();

    // setup Algorithm
    GaussianUniformMixture<DoubleVector> gaussianUniformMixture = ClassGenericsUtil.parameterizeOrAbort(GaussianUniformMixture.class, params);
    testParameterizationOk(params);

    // run GaussianUniformMixture on database
    OutlierResult result = gaussianUniformMixture.run(db);

    testSingleScore(result, 1025, -14.61080862);
    testAUC(db, "Noise", result, 0.928923076);
  }
}