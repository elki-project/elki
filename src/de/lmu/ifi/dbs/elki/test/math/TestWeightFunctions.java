package de.lmu.ifi.dbs.elki.test.math;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.*;

/**
 * JUnit test to assert consistency of a couple of Weight functions
 * @author Erich Schubert
 *
 */
public class TestWeightFunctions {
  /**
   * Just a 'boring' value test for completeness.
   */
  @Test
  public void testGetWeight() {
    WeightFunction[] wf = {new ConstantWeight(), new ErfcWeight(), new ErfcStddevWeight(),
        new GaussWeight(), new GaussStddevWeight(), new LinearWeight(), new ExponentialWeight()};
    double[] at0 = {1.0, 1.0, 1.0, 1.0, 0.3989422804014327, 1.0, 1.0};
    double[] at01 = {1.0, 0.8693490686884612, 0.920344325445942, 0.9772372209558107,
        0.3969525474770118, 0.91, 0.7943282347242815};
    double[] at09 = {1.0, 0.13877499454059491, 0.368120250693519, 0.15488166189124816,
        0.2660852498987548, 0.18999999999999995, 0.12589254117941673};
    double[] at10 = {1.0, 0.10000000000000016, 0.31731050786291415, 0.10000000000000002,
        0.24197072451914337, 0.09999999999999998, 0.10000000000000002};
    
    assert(wf.length == at0.length);
    assert(wf.length == at01.length);
    assert(wf.length == at09.length);
    assert(wf.length == at10.length);
    
    for (int i=0; i < wf.length; i++) {
      double val0 = wf[i].getWeight(0, 1, 1);
      double val01 = wf[i].getWeight(0.1, 1, 1);
      double val09 = wf[i].getWeight(0.9, 1, 1);
      double val10 = wf[i].getWeight(1.0, 1, 1);
      assertEquals(wf[i].getClass().getSimpleName()+" at 0.0", at0[i], val0, Double.MIN_VALUE);
      assertEquals(wf[i].getClass().getSimpleName()+" at 0.1", at01[i], val01, Double.MIN_VALUE);
      assertEquals(wf[i].getClass().getSimpleName()+" at 0.9", at09[i], val09, Double.MIN_VALUE);
      assertEquals(wf[i].getClass().getSimpleName()+" at 1.0", at10[i], val10, Double.MIN_VALUE);
    }
  }

}
