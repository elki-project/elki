package experimentalcode.lucia;


import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.GaussianUniformMixture;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the GaussianUniformMixture algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestGaussianUniformMixture extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";


  @Test
  public void testGaussianUniformMixture() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 1025, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();

    //setup Algorithm
    GaussianUniformMixture<DoubleVector> gaussianUniformMixture = ClassGenericsUtil.parameterizeOrAbort(GaussianUniformMixture.class, params);
    testParameterizationOk(params);

    //run GaussianUniformMixture on database
    OutlierResult result = gaussianUniformMixture.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 141
    int id = 141;
    testSingleScore(result, id, -12.771522154956983);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.992225641025641);
    
  }
}
