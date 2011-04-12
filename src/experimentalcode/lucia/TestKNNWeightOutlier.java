package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the KNNWeightOutlier algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestKNNWeightOutlier extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 5;

  
  @Test
  public void testKNNWeightOutlier() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(KNNWeightOutlier.K_ID, k);

    //setup Algorithm
    KNNWeightOutlier<DoubleVector, DoubleDistance> knnWeightOutlier = ClassGenericsUtil.parameterizeOrAbort(KNNWeightOutlier.class, params);
    testParameterizationOk(params);

    //run KNNWeightOutlier on database
    OutlierResult result = knnWeightOutlier.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, 2.384117261027324);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9912777777777778);
    
  }
}
