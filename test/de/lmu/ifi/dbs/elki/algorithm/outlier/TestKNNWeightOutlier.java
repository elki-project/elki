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
 * Tests the KNNWeightOutlier algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestKNNWeightOutlier extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testKNNWeightOutlier() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(KNNWeightOutlier.K_ID, 5);

    // setup Algorithm
    KNNWeightOutlier<DoubleVector, DoubleDistance> knnWeightOutlier = ClassGenericsUtil.parameterizeOrAbort(KNNWeightOutlier.class, params);
    testParameterizationOk(params);

    // run KNNWeightOutlier on database
    OutlierResult result = knnWeightOutlier.run(db);

    testSingleScore(result, 945, 2.384117261027324);
    testAUC(db, "Noise", result, 0.9912777777777778);
  }
}