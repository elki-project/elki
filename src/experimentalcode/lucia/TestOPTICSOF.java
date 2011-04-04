package experimentalcode.lucia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OPTICSOF;
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
 * Tests the OPTICS-OF algorithm. 
 * @author lucia
 * 
 */
public class TestOPTICSOF extends OutlierTest{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/parabel.csv";
  static int minPts = 22;


  @Test
  public void testOPTICSOF() throws UnableToComplyException {

    //Get Database
    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(OPTICS.MINPTS_ID, minPts);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

    // run OPTICS-OF
    OutlierResult result = runOPTICSOF(db, params);
    AnnotationResult<Double> scores = result.getScores();


    //check Outlier Score of Point 169
    int id = 169;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", 1.6108343626651815, score, 0.0001);

    
    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
//      System.out.println("OPTICS-OF(MinPts="+ minPts + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.9058, actual, 0.00001);
    }
    
  }


  private static OutlierResult runOPTICSOF(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    OPTICSOF<DoubleVector, DoubleDistance> opticsof = null;
    Class<OPTICSOF<DoubleVector, DoubleDistance>> opticsofcls = ClassGenericsUtil.uglyCastIntoSubclass(OPTICSOF.class);
    opticsof = params.tryInstantiate(opticsofcls, opticsofcls);
    params.failOnErrors();

    // run OPTICSOF on database
    return opticsof.run(db);
  }

}
