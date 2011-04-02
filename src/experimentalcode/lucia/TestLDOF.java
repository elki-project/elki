package experimentalcode.lucia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LDOF;
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

/**
 * Tests the LDOF algorithm. 
 * @author lucia
 * 
 */
public class TestLDOF extends OutlierTest implements JUnit4Test{
  // the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";
  static int k = 25;
  static int interesstingPoint_id141 = 141;

  @Test
  public void testLDOF() throws UnableToComplyException {
    ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

    Database<DoubleVector> db = getDatabase(dataset);


    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LDOF.K_ID, k);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");


    // run LDOF
    OutlierResult result = runLDOF(db, params);
    AnnotationResult<Double> scores = result.getScores();

    double outlierScore_p141;
    int intId;
    for(DBID id : db.getIDs()) {
      pair_scoresIds.add(new Pair<Double, DBID>(scores.getValueFor(id),id));
      intId = id.getID().getIntegerID();
      if(intId == interesstingPoint_id141){
        outlierScore_p141 = scores.getValueFor(id.getID());
        System.out.println("Outlier Score of the Point with id " + intId + ": " + outlierScore_p141);
        Assert.assertEquals("Outlier Score of Point with id " + intId + " not right.", 0.8976, outlierScore_p141, 0.0001);

      }
    }

    //get ROC AUC
    List<Double> auc = getROCAUC(db, result, params);
    Iterator<Double> iter = auc.listIterator();
    double actual;
    while(iter.hasNext()){
      actual = iter.next();
      System.out.println("LDOF(k="+ k + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.96379487, actual, 0.00001);
    }
  }


  private static OutlierResult runLDOF(Database<DoubleVector> db, ListParameterization params) {
    // setup algorithm
    LDOF<DoubleVector, DoubleDistance> ldof = null;
    Class<LDOF<DoubleVector, DoubleDistance>> ldofcls = ClassGenericsUtil.uglyCastIntoSubclass(LDOF.class);
    ldof = params.tryInstantiate(ldofcls, ldofcls);
    params.failOnErrors();

    // run LDOF on database
    return ldof.run(db);
  }
}
