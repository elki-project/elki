package de.lmu.ifi.dbs.elki.test.math;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions.*;

public class TestWeightFunctions {
  /**
   * Just a 'boring' value test for completeness.
   */
  @Test
  public void testGetWeight() {
    WeightFunction[] wf = {new ConstantWeight(), new ErfcWeight(), new ErfcStddevWeight(),
        new GaussWeight(), new GaussStddevWeight(), new LinearWeight(), new ExponentialWeight()};
    double[] at0 = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    double[] at01 = {1.0, 0.7995676697105694, 0.920344325445942, 0.9772372209558107,
        0.9950124791926823, 0.91, 0.7943282347242815};
    double[] at09 = {1.0, 0.10981854852784237, 0.368120250693519, 0.15488166189124816,
        0.6669768108584744, 0.18999999999999995, 0.12589254117941673};
    
    assert(wf.length == at0.length);
    assert(wf.length == at01.length);
    assert(wf.length == at09.length);
    
    for (int i=0; i < wf.length; i++) {
      double val0 = wf[i].getWeight(0, 1, 1);
      double val01 = wf[i].getWeight(0.1, 1, 1);
      double val09 = wf[i].getWeight(0.9, 1, 1);
      assertTrue(val0 == at0[i]);
      assertTrue(val01 == at01[i]);
      assertTrue(val09 == at09[i]);
    }
  }

}
