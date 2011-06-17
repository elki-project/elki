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
 * Tests the DBOutlierScore algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestDBOutlierScore extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testDBOutlierScore() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(DBOutlierScore.D_ID, 0.175);

    // setup Algorithm
    DBOutlierScore<DoubleVector, DoubleDistance> dbOutlierScore = ClassGenericsUtil.parameterizeOrAbort(DBOutlierScore.class, params);
    testParameterizationOk(params);

    // run DBOutlierScore on database
    OutlierResult result = dbOutlierScore.run(db);

    testSingleScore(result, 1025, 0.688780487804878);
    testAUC(db, "Noise", result, 0.992565641);
  }
}