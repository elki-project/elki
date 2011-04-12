package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.INFLO;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the INFLO algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestINFLO extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testINFLO() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(INFLO.K_ID, 29);

    // setup Algorithm
    INFLO<DoubleVector, DoubleDistance> inflo = ClassGenericsUtil.parameterizeOrAbort(INFLO.class, params);
    testParameterizationOk(params);

    // run INFLO on database
    OutlierResult result = inflo.run(db);

    testSingleScore(result, 945, 2.5711647857619484);
    testAUC(db, "Noise", result, 0.935222);
  }
}