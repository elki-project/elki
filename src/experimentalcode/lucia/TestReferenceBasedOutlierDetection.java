package experimentalcode.lucia;

import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ReferenceBasedOutlierDetection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;


/**
 * Tests the ReferenceBasedOutlierDetection algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestReferenceBasedOutlierDetection extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 11;
  static int gridSize = 11;


  @Test
  public void testReferenceBasedOutlierDetection() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 960, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ReferenceBasedOutlierDetection.K_ID, k);
    params.addParameter(GridBasedReferencePoints.GRID_ID, gridSize);

    //setup Algorithm
    ReferenceBasedOutlierDetection<DoubleVector, DoubleDistance> referenceBasedOutlierDetection = ClassGenericsUtil.parameterizeOrAbort(ReferenceBasedOutlierDetection.class, params);
    testParameterizationOk(params);

    //run ReferenceBasedOutlierDetection on database
    OutlierResult result = referenceBasedOutlierDetection.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 273
    int id = 273;
    testSingleScore(result, id, 0.9260829537195538);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9892407407407409);
    
  }
}
