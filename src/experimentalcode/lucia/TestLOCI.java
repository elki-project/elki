package experimentalcode.lucia;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOCI;
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
 * Tests the LOCI algorithm. 
 * @author lucia
 * 
 */
public class TestLOCI extends OutlierTest implements JUnit4Test{
  // the following values depend on the data set used!
  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";
  static double rmax = 0.5;


  @Test
  public void testLOCI() throws UnableToComplyException {
    ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

    Database<DoubleVector> db = getDatabase(dataset);


    //Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOCI.RMAX_ID, rmax);
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");


    // run LoOP
    OutlierResult result = runLOCI(db, params);
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
      System.out.println("LOCI(rmax="+ rmax + ") ROC AUC: " + actual);
      Assert.assertEquals("ROC AUC not right.", 0.98844444, actual, 0.00001);
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
