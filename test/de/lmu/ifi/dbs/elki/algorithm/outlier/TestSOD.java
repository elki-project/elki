package experimentalcode.lucia;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.SOD;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the SOD algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestSOD extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testSOD() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(SOD.KNN_ID, 25);
    params.addParameter(SharedNearestNeighborPreprocessor.Factory.NUMBER_OF_NEIGHBORS_ID, 19);

    // setup Algorithm
    SOD<DoubleVector, DoubleDistance> sod = ClassGenericsUtil.parameterizeOrAbort(SOD.class, params);
    testParameterizationOk(params);

    // run SOD on database
    OutlierResult result = sod.run(db);

    testSingleScore(result, 1293, 1.7277777);
    testAUC(db, "Noise", result, 0.94956862);
  }
}