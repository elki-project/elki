package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the LoOP algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestLoOP extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 15;


  @Test
  public void testLoOP() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LoOP.KCOMP_ID, k);

    //setup Algorithm
    LoOP<DoubleVector, DoubleDistance> loop = ClassGenericsUtil.parameterizeOrAbort(LoOP.class, params);
    testParameterizationOk(params);

    //run LoOP on database
    OutlierResult result = loop.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, 0.39805457858293325);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9443796296296296);
    
  }
}
