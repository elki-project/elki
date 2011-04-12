package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuEvolutionary;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the AggarwalYuEvolutionary algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestAggarwalYuEvolutionary extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 2;
  static int phi = 8;
  static int m = 5;


  @Test
  public void testAggarwalYuEvolutionary() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuEvolutionary.K_ID, k);
    params.addParameter(AggarwalYuEvolutionary.PHI_ID, phi);
    params.addParameter(AggarwalYuEvolutionary.M_ID, m);
    params.addParameter(AggarwalYuEvolutionary.SEED_ID, 0);

    //setup Algorithm
    AggarwalYuEvolutionary<DoubleVector> aggarwalYuEvolutionary = ClassGenericsUtil.parameterizeOrAbort(AggarwalYuEvolutionary.class, params);
    testParameterizationOk(params);

    //run AggarwalYuEvolutionary on database
    OutlierResult result = aggarwalYuEvolutionary.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, 16.6553612449883);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.5788796296296297);
    
  }
}
