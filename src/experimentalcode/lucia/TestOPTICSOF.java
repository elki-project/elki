package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OPTICSOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the OPTICS-OF algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestOPTICSOF extends AbstractSimpleAlgorithmTest implements JUnit4Test {

  static final String dataset = "src/experimentalcode/lucia/datensaetze/parabel.csv";

  @Test
  public void testOPTICSOF() throws ParameterException {
    // get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 530, paramsDB);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(OPTICS.MINPTS_ID, 22);

    // setup Algorithm
    OPTICSOF<DoubleVector, DoubleDistance> opticsof = ClassGenericsUtil.parameterizeOrAbort(OPTICSOF.class, params);
    testParameterizationOk(params);

    // run OPTICSOF on database
    OutlierResult result = opticsof.run(db);

    testSingleScore(result, 169, 1.6108343626651815);
    testAUC(db, "Noise", result, 0.9058);
  }
}