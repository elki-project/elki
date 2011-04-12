package experimentalcode.lucia;


import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.DBOutlierScore;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the DBOutlierScore algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestDBOutlierScore extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";
  static double d = 0.175;


  @Test
  public void testDBOutlierScore() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 1025, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(DBOutlierScore.D_ID, d);

    //setup Algorithm
    DBOutlierScore<DoubleVector, DoubleDistance> dbOutlierScore = ClassGenericsUtil.parameterizeOrAbort(DBOutlierScore.class, params);
    testParameterizationOk(params);

    //run DBOutlierScore on database
    OutlierResult result = dbOutlierScore.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 141
    int id = 141;
    testSingleScore(result, id, 0.688780487804878);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.992697435897436);

  }
}
