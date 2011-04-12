package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LDOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the LDOF algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestLDOF extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";
  static int k = 25;


  @Test
  public void testLDOF() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 1025, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LDOF.K_ID, k);

    //setup Algorithm
    LDOF<DoubleVector, DoubleDistance> ldof = ClassGenericsUtil.parameterizeOrAbort(LDOF.class, params);
    testParameterizationOk(params);

    //run LDOF on database
    OutlierResult result = ldof.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 141
    int id = 141;
    testSingleScore(result, id, 0.8976268846182947);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9637948717948718);
    
  }
}
