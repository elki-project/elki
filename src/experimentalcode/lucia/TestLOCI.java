package experimentalcode.lucia;


import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOCI;
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
 * Tests the LOCI algorithm. 
 * @author lucia
 * 
 */
public class TestLOCI extends OutlierTest{
  // the following values depend on the data set used!
  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";
  static double rmax = 0.35;


  @Test
  public void testLOCI() throws UnableToComplyException {

    //get database
    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOCI.RMAX_ID, rmax);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

    // run LOCI
    OutlierResult result = runLOCI(db, params);
    AnnotationResult<Double> scores = result.getScores();

    
    //check Outlier Score of Point 275
    int id = 275;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", 3.805438242211049, score, 0.0001);

    
    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
//      System.out.println("LOCI(rmax="+ rmax + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.98966667, actual, 0.00001);
    }
    
  }


  
  private static OutlierResult runLOCI(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    LOCI<DoubleVector, DoubleDistance> loci = null;
    Class<LOCI<DoubleVector, DoubleDistance>> locicls = ClassGenericsUtil.uglyCastIntoSubclass(LOCI.class);
    loci = params.tryInstantiate(locicls, locicls);
    params.failOnErrors();

    // run LOCI on database
    return loci.run(db);
  }

}
