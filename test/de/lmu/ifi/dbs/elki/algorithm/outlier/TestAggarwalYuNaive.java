package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuNaive;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the AggarwalYuNaive algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestAggarwalYuNaive extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testAggarwalYuNaive() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuNaive.K_ID, 2);
    params.addParameter(AggarwalYuNaive.PHI_ID, 8);

    // setup Algorithm
    AggarwalYuNaive<DoubleVector> aggarwalYuNaive = ClassGenericsUtil.parameterizeOrAbort(AggarwalYuNaive.class, params);
    testParameterizationOk(params);

    // run AggarwalYuNaive on database
    OutlierResult result = aggarwalYuNaive.run(db);

    testSingleScore(result, 945, -2.3421601750764798);
    testAUC(db, "Noise", result, 0.8643148148);
  }
}