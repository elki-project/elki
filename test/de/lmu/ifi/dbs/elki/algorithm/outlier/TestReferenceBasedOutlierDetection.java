package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ReferenceBasedOutlierDetection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;

/**
 * Tests the ReferenceBasedOutlierDetection algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestReferenceBasedOutlierDetection extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testReferenceBasedOutlierDetection() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ReferenceBasedOutlierDetection.K_ID, 11);
    params.addParameter(GridBasedReferencePoints.GRID_ID, 11);

    // setup Algorithm
    ReferenceBasedOutlierDetection<DoubleVector, DoubleDistance> referenceBasedOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(ReferenceBasedOutlierDetection.class, params);
    testParameterizationOk(params);

    // run ReferenceBasedOutlierDetection on database
    OutlierResult result = referenceBasedOutlierDetection.run(db);

    testSingleScore(result, 945, 0.9260829537195538);
    testAUC(db, "Noise", result, 0.9892407407407409);
  }
}