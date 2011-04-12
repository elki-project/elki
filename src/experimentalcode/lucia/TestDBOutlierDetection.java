package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.DBOutlierDetection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the DBOutlierDetection algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestDBOutlierDetection extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";
  static double d = 0.175;
  static double p = 0.98;

  
  @Test
  public void testDBOutlierDetection() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 1025, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(DBOutlierDetection.D_ID, d);
    params.addParameter(DBOutlierDetection.P_ID, p);

    //setup Algorithm
    DBOutlierDetection<DoubleVector, DoubleDistance> dbOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(DBOutlierDetection.class, params);
    testParameterizationOk(params);

    
    //run DBOutlierDetection on database
    OutlierResult result = dbOutlierDetection.run(db);
    db.getHierarchy().add(db, result);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9744717948717949);
    
  }
}
