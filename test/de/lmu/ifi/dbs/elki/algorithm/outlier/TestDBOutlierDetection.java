package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.DBOutlierDetection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the DBOutlierDetection algorithm. 
 * 
 * @author Lucia Cichella
 */
public class TestDBOutlierDetection extends AbstractSimpleAlgorithmTest implements JUnit4Test{
  @Test
  public void testDBOutlierDetection() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(DBOutlierDetection.D_ID, 0.175);
    params.addParameter(DBOutlierDetection.P_ID, 0.98);

    //setup Algorithm
    DBOutlierDetection<DoubleVector, DoubleDistance> dbOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(DBOutlierDetection.class, params);
    testParameterizationOk(params);

    //run DBOutlierDetection on database
    OutlierResult result = dbOutlierDetection.run(db);

    testSingleScore(result, 1025, 0.0);
    testAUC(db, "Noise", result, 0.97386666);   
  }
}