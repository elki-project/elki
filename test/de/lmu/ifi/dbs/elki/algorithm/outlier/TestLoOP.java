package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the LoOP algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestLoOP extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testLoOP() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LoOP.KCOMP_ID, 15);

    // setup Algorithm
    LoOP<DoubleVector, DoubleDistance> loop = ClassGenericsUtil.parameterizeOrAbort(LoOP.class, params);
    testParameterizationOk(params);

    // run LoOP on database
    OutlierResult result = loop.run(db);

    testSingleScore(result, 945, 0.39805457858293325);
    testAUC(db, "Noise", result, 0.9443796296296296);
  }
}