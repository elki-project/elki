package experimentalcode.lucia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.INFLO;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the INFLO algorithm. 
 * @author lucia
 * 
 */
public class TestINFLO extends OutlierTest{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 29;


  @Test
  public void testINFLO() throws UnableToComplyException {

    //Get database
    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(INFLO.K_ID, k);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

    // run INFLO
    OutlierResult result = runINFLO(db, params);
    AnnotationResult<Double> scores = result.getScores();


    //check Outlier Score of Point 273
    int id = 273;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", 2.5711647857619484, score, 0.0001);

    
    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
//      System.out.println("INFLO(k="+ k + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.94244444, actual, 0.00001);
    }
  }

  
  private static OutlierResult runINFLO(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    INFLO<DoubleVector, DoubleDistance> inflo = null;
    Class<INFLO<DoubleVector, DoubleDistance>> inflocls = ClassGenericsUtil.uglyCastIntoSubclass(INFLO.class);
    inflo = params.tryInstantiate(inflocls, inflocls);
    params.failOnErrors();

    // run INFLO on database
    return inflo.run(db);
  }

}
