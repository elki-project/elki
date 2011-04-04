package experimentalcode.lucia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuNaive;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the AggarwalYuNaive algorithm. 
 * @author lucia
 */
public class TestAggarwalYuNaive extends OutlierTest{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";

  static int k = 2;
  static int phi = 8;


  @Test
  public void testAggarwalYuNaive() throws UnableToComplyException {

    //Get Database
    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuNaive.K_ID, k);
    params.addParameter(AggarwalYuNaive.PHI_ID, phi);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

    // run AggarwalYuNaive
    OutlierResult result = runAggarwalYuNaive(db, params);
    AnnotationResult<Double> scores = result.getScores();


    //check Outlier Score of Point 273
    int id = 273;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", -2.3421601750764798, score, 0.0001);


    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
//      System.out.println("AggarwalYuNaive(k="+ k + " und phi="+phi+") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.85966667, actual, 0.00001);
    }
  }


  
  private static OutlierResult runAggarwalYuNaive(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    AggarwalYuNaive<DoubleVector> aggarwalYuNaive = null;
    Class<AggarwalYuNaive<DoubleVector>> aggarwalYuNaivecls = ClassGenericsUtil.uglyCastIntoSubclass(AggarwalYuNaive.class);
    aggarwalYuNaive = params.tryInstantiate(aggarwalYuNaivecls, aggarwalYuNaivecls);
    params.failOnErrors();

    // run AggarwalYuNaive on database
    return aggarwalYuNaive.run(db);
  }
}
