package experimentalcode.lucia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.GaussianModel;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Tests the GaussianModel algorithm. 
 * @author lucia
 * 
 */
public class TestGaussianModel extends OutlierTest implements JUnit4Test{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";


  @Test
  public void testGaussianModel() throws UnableToComplyException {
    ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");


    // run GaussianModel
    OutlierResult result = runGaussianModel(db, params);
    AnnotationResult<Double> scores = result.getScores();

    for(DBID id : db.getIDs()) {
      pair_scoresIds.add(new Pair<Double, DBID>(scores.getValueFor(id),id));
    }

    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
      System.out.println("GaussianModel ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.99376410, actual, 0.00001);
    }
  }



  private static OutlierResult runGaussianModel(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    GaussianModel<DoubleVector> gaussianModel = null;
    Class<GaussianModel<DoubleVector>> gaussianModelcls = ClassGenericsUtil.uglyCastIntoSubclass(GaussianModel.class);
    gaussianModel = params.tryInstantiate(gaussianModelcls, gaussianModelcls);
    params.failOnErrors();

    // run GaussianModel on database
    return gaussianModel.run(db);
  }


}
