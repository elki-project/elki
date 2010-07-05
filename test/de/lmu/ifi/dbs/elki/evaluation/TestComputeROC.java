package de.lmu.ifi.dbs.elki.evaluation;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 *
 */
public class TestComputeROC implements JUnit4Test {
  /**
   * Test ROC curve generation, including curve simplification
   */
  @Test
  public void testROCCurve() {
    ModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));
    
    ArrayList<Pair<Double, DBID>> distances = new ArrayList<Pair<Double, DBID>>();
    distances.add(new Pair<Double, DBID>(0.0, DBIDUtil.importInteger(1)));
    distances.add(new Pair<Double, DBID>(1.0, DBIDUtil.importInteger(2)));
    distances.add(new Pair<Double, DBID>(2.0, DBIDUtil.importInteger(6)));
    distances.add(new Pair<Double, DBID>(3.0, DBIDUtil.importInteger(7)));
    distances.add(new Pair<Double, DBID>(3.0, DBIDUtil.importInteger(3)));
    distances.add(new Pair<Double, DBID>(4.0, DBIDUtil.importInteger(8)));
    distances.add(new Pair<Double, DBID>(4.0, DBIDUtil.importInteger(4)));
    distances.add(new Pair<Double, DBID>(5.0, DBIDUtil.importInteger(9)));
    distances.add(new Pair<Double, DBID>(6.0, DBIDUtil.importInteger(5)));
    
    List<DoubleDoublePair> roccurve = ROC.materializeROC(9, positive, distances.iterator());
    //System.out.println(roccurve);
    Assert.assertEquals("ROC curve too complex", 5, roccurve.size());
    
    double auc = ROC.computeAUC(roccurve);
    Assert.assertEquals("ROC AUC not right.", 0.5, auc, 0.0001);
  }
}
