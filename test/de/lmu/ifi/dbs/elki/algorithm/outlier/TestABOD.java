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
 * Tests the ABOD algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestABOD extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testABOD() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ABOD.K_ID, 5);

    // setup Algorithm
    ABOD<DoubleVector> abod = ClassGenericsUtil.parameterizeOrAbort(ABOD.class, params);
    testParameterizationOk(params);

    // run ABOD on database
    OutlierResult result = abod.run(db);

    testSingleScore(result, 945, 3.7108897864090475E-4);
    testAUC(db, "Noise", result, 0.9638148148148148);
  }
}