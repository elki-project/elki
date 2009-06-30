package de.lmu.ifi.dbs.elki.test.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.test.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 *
 */
public class TestComputeROC implements JUnit4Test {
  @Test
  public void testROCCurve() {
    HashSet<Integer> positive = new HashSet<Integer>();
    positive.add(1);
    positive.add(2);
    positive.add(3);
    positive.add(4);
    positive.add(5);
    
    ArrayList<Pair<Double, Integer>> distances = new ArrayList<Pair<Double, Integer>>();
    distances.add(new Pair<Double, Integer>(0.0,1));
    distances.add(new Pair<Double, Integer>(1.0,2));
    distances.add(new Pair<Double, Integer>(2.0,6));
    distances.add(new Pair<Double, Integer>(3.0,7));
    distances.add(new Pair<Double, Integer>(3.0,3));
    distances.add(new Pair<Double, Integer>(4.0,8));
    distances.add(new Pair<Double, Integer>(4.0,4));
    distances.add(new Pair<Double, Integer>(5.0,9));
    distances.add(new Pair<Double, Integer>(6.0,5));
    
    List<Pair<Double, Double>> roccurve = ROC.materializeROC(9, positive, distances.iterator());
    //System.out.println(roccurve);
    Assert.assertEquals("ROC curve too complex", 5, roccurve.size());
    
    double auc = ROC.computeAUC(roccurve);
    Assert.assertEquals("ROC AUC not right.", 0.5, auc, 0.0001);
  }
}
