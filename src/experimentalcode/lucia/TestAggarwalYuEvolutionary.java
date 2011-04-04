package experimentalcode.lucia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuEvolutionary;
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
 * Tests the AggarwalYuEvolutionary algorithm. 
 * @author lucia
 * 
 */
public class TestAggarwalYuEvolutionary extends OutlierTest{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";

  static int k = 2;
  static int phi = 8;
  static int m = 5;


  @Test
  public void testAggarwalYuEvolutionary() throws UnableToComplyException {
    //	  TODO: AggarwalYuEvolutionary um einen Seed Parameter erweitern, damit Ergebnisse stabil

    //get Database
    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuEvolutionary.K_ID, k);
    params.addParameter(AggarwalYuEvolutionary.PHI_ID, phi);
    params.addParameter(AggarwalYuEvolutionary.M_ID, m);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

    // run AggarwalYuEvolutionary
    OutlierResult result = runAggarwalYuEvolutionary(db, params);
    AnnotationResult<Double> scores = result.getScores();


    //check Outlier Score of Point 273
    int id = 273;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", 16.6553612449883, score, 0.0001);

    
    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
//      System.out.println("AggarwalYuEvolutionary(k="+ k + " und phi="+phi+" und m="+m+") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.56398148, actual, 0.00001);
    }
  }

  private static OutlierResult runAggarwalYuEvolutionary(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    AggarwalYuEvolutionary<DoubleVector> aggarwalYuEvolutionary = null;
    Class<AggarwalYuEvolutionary<DoubleVector>> aggarwalYuEvolutionarycls = ClassGenericsUtil.uglyCastIntoSubclass(AggarwalYuEvolutionary.class);
    aggarwalYuEvolutionary = params.tryInstantiate(aggarwalYuEvolutionarycls, aggarwalYuEvolutionarycls);
    params.failOnErrors();

    // run AggarwalYuEvolutionary on database
    return aggarwalYuEvolutionary.run(db);
  }
}
