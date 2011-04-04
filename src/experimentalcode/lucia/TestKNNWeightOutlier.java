package experimentalcode.lucia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier;
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
 * Tests the KNNWeightOutlier algorithm. 
 * @author lucia
 * 
 */
public class TestKNNWeightOutlier extends OutlierTest{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
  static int k = 5;

  @Test
  public void testKNNWeightOutlier() throws UnableToComplyException {

    //Get database
    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(KNNWeightOutlier.K_ID, k);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");


    // run KNNWeightOutlier
    OutlierResult result = runKNNWeight(db, params);
    AnnotationResult<Double> scores = result.getScores();


    //check Outlier Score of Point 273
    int id = 273;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", 2.384117261027324, score, 0.0001);


    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
//      System.out.println("KNNWeightOutlier(k="+ k + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.99127778, actual, 0.00001);
    }
  }

  
  private static OutlierResult runKNNWeight(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    KNNWeightOutlier<DoubleVector, DoubleDistance> kNNWeightOutlier = null;
    Class<KNNWeightOutlier<DoubleVector, DoubleDistance>> knncls = ClassGenericsUtil.uglyCastIntoSubclass(KNNWeightOutlier.class);
    kNNWeightOutlier = params.tryInstantiate(knncls, knncls);
    params.failOnErrors();

    // run KNNWeightOutlier on database
    return kNNWeightOutlier.run(db);
  }

}
