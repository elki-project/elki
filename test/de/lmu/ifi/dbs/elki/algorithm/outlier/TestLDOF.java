package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the LDOF algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestLDOF extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testLDOF() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LDOF.K_ID, 25);

    // setup Algorithm
    LDOF<DoubleVector, DoubleDistance> ldof = ClassGenericsUtil.parameterizeOrAbort(LDOF.class, params);
    testParameterizationOk(params);

    // run LDOF on database
    OutlierResult result = ldof.run(db);

    testSingleScore(result, 1025, 0.8976268846182947);
    testAUC(db, "Noise", result, 0.9637948717948718);
  }
}