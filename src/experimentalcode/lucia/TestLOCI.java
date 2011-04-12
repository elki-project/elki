package experimentalcode.lucia;


import org.junit.Test;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOCI;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the LOCI algorithm. 
 * @author Lucia Cichella
 * 
 */
public class TestLOCI extends AbstractSimpleAlgorithmTest implements JUnit4Test{

  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";
  static double rmax = 0.35;


  @Test
  public void testLOCI() throws ParameterException {
    //get Database
    ListParameterization paramsDB = new ListParameterization();
    paramsDB.addParameter(FileBasedDatabaseConnection.SEED_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(dataset, 330, paramsDB);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOCI.RMAX_ID, rmax);

    //setup Algorithm
    LOCI<DoubleVector, DoubleDistance> loci = ClassGenericsUtil.parameterizeOrAbort(LOCI.class, params);
    testParameterizationOk(params);

    //run LOCI on database
    OutlierResult result = loci.run(db);
    db.getHierarchy().add(db, result);


    //check Outlier Score of Point 275
    int id = 275;
    testSingleScore(result, id, 3.805438242211049);

    //test ROC AUC
    testAUC(db, "Noise", result, 0.9896666666666667);
    
  }
}
