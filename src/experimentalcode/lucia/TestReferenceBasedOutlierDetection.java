package experimentalcode.lucia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ReferenceBasedOutlierDetection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;


/**
 * Tests the ReferenceBasedOutlierDetection algorithm. 
 * @author lucia
 * 
 */
public class TestReferenceBasedOutlierDetection extends OutlierTest implements JUnit4Test{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";

  static int k = 11;
  static int gridSize = 11;



  @Test
  public void testReferenceBasedOutlierDetection() throws UnableToComplyException {
    ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

    Database<DoubleVector> db = getDatabase(dataset);

    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(ReferenceBasedOutlierDetection.K_ID, k);
    params.addParameter(GridBasedReferencePoints.GRID_ID, gridSize);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");


    // run ReferenceBasedOutlierDetection
    OutlierResult result = runReferenceBasedOutlierDetection(db, params);
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
      System.out.println("ReferenceBasedOutlierDetection (k="+ k + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.98924074, actual, 0.00001);
    }
  }

  private static OutlierResult runReferenceBasedOutlierDetection(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    ReferenceBasedOutlierDetection<DoubleVector, DoubleDistance> referenceBasedOutlierDetection = null;
    Class<ReferenceBasedOutlierDetection<DoubleVector, DoubleDistance>> referenceBasedOutlierDetectioncls = ClassGenericsUtil.uglyCastIntoSubclass(ReferenceBasedOutlierDetection.class);
    referenceBasedOutlierDetection = params.tryInstantiate(referenceBasedOutlierDetectioncls, referenceBasedOutlierDetectioncls);
    params.failOnErrors();

    // run ReferenceBasedOutlierDetection on database
    return referenceBasedOutlierDetection.run(db);
  }

}
