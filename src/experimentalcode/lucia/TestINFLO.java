package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.INFLO;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the INFLO algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestINFLO extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 29;

  
  @Test
  public void testINFLO() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(INFLO.K_ID, k);

    //setup Algorithm
    INFLO<DoubleVector, DoubleDistance> inflo = ClassGenericsUtil.parameterizeOrAbort(INFLO.class, params);
    testParameterizationOk(params);

    //run INFLO on database
    OutlierResult result = inflo.run(db);
    db.getHierarchy().add(db, result);

    
    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, 2.5711647857619484);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9438981481481482);
    
  }
}
