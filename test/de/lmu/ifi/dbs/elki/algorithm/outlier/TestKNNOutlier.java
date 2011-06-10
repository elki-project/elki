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
 * Tests the KNNOutlier algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestKNNOutlier extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testKNNOutlier() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(KNNOutlier.K_ID, 2);

    // setup Algorithm
    KNNOutlier<DoubleVector, DoubleDistance> knnOutlier = ClassGenericsUtil.parameterizeOrAbort(KNNOutlier.class, params);
    testParameterizationOk(params);

    // run KNNOutlier on database
    OutlierResult result = knnOutlier.run(db);

    testSingleScore(result, 945, 0.4793554700168577);
    testAUC(db, "Noise", result, 0.991462962962963);
  }
}