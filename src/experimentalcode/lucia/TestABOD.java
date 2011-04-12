package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ABOD;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the ABOD algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestABOD extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 5;

  
  @Test
  public void testABOD() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ABOD.K_ID, k);

    //setup Algorithm
    ABOD<DoubleVector> abod = ClassGenericsUtil.parameterizeOrAbort(ABOD.class, params);
    testParameterizationOk(params);

    //run ABOD on database
    OutlierResult result = abod.run(db);
    db.getHierarchy().add(db, result);

    
    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, 3.7108897864090475E-4);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9638148148148148);
    
  }
}
