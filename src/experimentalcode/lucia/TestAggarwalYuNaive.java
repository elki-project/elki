package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuNaive;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the AggarwalYuNaive algorithm. 
 * @author Lucia Cichella
 */
public class TestAggarwalYuNaive extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 2;
  static int phi = 8;


  @Test
  public void testAggarwalYuNaive() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuNaive.K_ID, k);
    params.addParameter(AggarwalYuNaive.PHI_ID, phi);

    //setup Algorithm
    AggarwalYuNaive<DoubleVector> aggarwalYuNaive = ClassGenericsUtil.parameterizeOrAbort(AggarwalYuNaive.class, params);
    testParameterizationOk(params);

    //run AggarwalYuNaive on database
    OutlierResult result = aggarwalYuNaive.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, -2.3421601750764798);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.8648796296296297);

  }
}
