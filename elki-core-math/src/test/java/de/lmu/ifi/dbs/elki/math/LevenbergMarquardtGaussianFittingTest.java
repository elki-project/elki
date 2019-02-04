/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.linearalgebra.fitting.GaussianFittingFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.fitting.LevenbergMarquardtMethod;

/**
 * Test to evaluate Levenberg-Marquardt fitting on a given Gaussian
 * distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 */
public class LevenbergMarquardtGaussianFittingTest {
  /**
   * Evaluate on a symmetric Gaussian distribution. Traditional estimation
   * already has the mean quite good, but is far off on the stddev. The improved
   * fitting is much better on the stddev.
   */
  // these points were generated with mean 0.12345 and stddev 0.98765
  @Test
  public void testSymmetric() {
    double[] testx = { -0.787791050195, -0.738026373791, -0.688261697386, -0.638497020982, -0.588732344578, -0.538967668174, -0.489202991769, -0.439438315365, -0.389673638961, -0.339908962556, -0.290144286152, -0.240379609748, -0.190614933344, -0.140850256939, -0.0910855805352, -0.0413209041309, 0.00844377227332, 0.0582084486776, 0.107973125082, 0.157737801486, 0.20750247789, 0.257267154295, 0.307031830699, 0.356796507103, 0.406561183507, 0.456325859912, 0.506090536316, 0.55585521272, 0.605619889124, 0.655384565529, 0.705149241933, 0.754913918337, 0.804678594741, 0.854443271146, 0.90420794755, 0.953972623954, 1.00373730036, 1.05350197676, 1.10326665317 };
    double[] testy = { 0.25319163934, 0.210993032783, 0.26122946916, 0.301418618261, 0.309456448082, 0.319503735357, 0.327541565177, 0.285342958621, 0.371749629189, 0.345626682273, 0.357683427004, 0.343617224818, 0.365721256824, 0.363711799369, 0.39586311865, 0.389834746285, 0.456146842302, 0.434042810296, 0.39586311865, 0.40390094847, 0.442080640117, 0.375768544099, 0.355673969549, 0.373759086644, 0.39586311865, 0.371749629189, 0.345626682273, 0.361702341914, 0.381796916465, 0.357683427004, 0.405910405925, 0.353664512093, 0.349645597183, 0.267257841525, 0.263238926615, 0.313475362992, 0.243144352064, 0.25721055425, 0.221040320058 };
    double mean = 0.122895805963;
    double stddev = 0.542856090502;
    double[] s = new double[testx.length];
    for(int i = 0; i < testx.length; i++) {
      s[i] = 1.0;
    }
    double[] params = { mean, stddev, 1 };
    boolean[] dofit = { true, true, false };
    LevenbergMarquardtMethod fit = new LevenbergMarquardtMethod(GaussianFittingFunction.STATIC, params, dofit, testx, testy, s);
    for(int i = 0; i < 50; i++) {
      fit.iterate();
    }
    double[] ps = fit.getParams();
    // compare results.
    double[] should = { 0.1503920, 0.9788814, 1 };
    assertEquals("Mean doesn't match.", should[0], ps[0], 0.0001);
    assertEquals("Stddev doesn't match.", should[1], ps[1], 0.0001);
    assertEquals("Scaling doesn't match.", should[2], ps[2], 0.0001);
  }

  /**
   * Same experiment, but only with one leg of the distribution. This results in
   * the traditional mean being far off.
   */
  @Test
  public void testAsymmetric() {
    double[] testx = { 0.157737801486, 0.20750247789, 0.257267154295, 0.307031830699, 0.356796507103, 0.406561183507, 0.456325859912, 0.506090536316, 0.55585521272, 0.605619889124, 0.655384565529, 0.705149241933, 0.754913918337, 0.804678594741, 0.854443271146, 0.90420794755, 0.953972623954, 1.00373730036, 1.05350197676, 1.10326665317, 1.15303132957, 1.20279600598, 1.25256068238, 1.30232535878, 1.35209003519, 1.40185471159, 1.451619388, 1.5013840644, 1.55114874081, 1.60091341721, 1.65067809361, 1.70044277002, 1.75020744642, 1.79997212283, 1.84973679923, 1.89950147564, 1.94926615204, 1.99903082844, 2.04879550485, 2.09856018125, 2.14832485766, 2.19808953406, 2.24785421046, 2.29761888687, 2.34738356327, 2.39714823968, 2.44691291608, 2.49667759249, 2.54644226889, 2.59620694529, 2.6459716217, 2.6957362981, 2.74550097451, 2.79526565091, 2.84503032732, 2.89479500372, 2.94455968012, 2.99432435653, 3.04408903293, 3.09385370934 };
    double[] testy = { 0.40390094847, 0.442080640117, 0.375768544099, 0.355673969549, 0.373759086644, 0.39586311865, 0.371749629189, 0.345626682273, 0.361702341914, 0.381796916465, 0.357683427004, 0.405910405925, 0.353664512093, 0.349645597183, 0.267257841525, 0.263238926615, 0.313475362992, 0.243144352064, 0.25721055425, 0.221040320058, 0.247163266974, 0.219030862603, 0.267257841525, 0.186879543322, 0.184870085867, 0.160756596406, 0.202955202963, 0.132624192035, 0.150709309131, 0.158747138951, 0.100472872754, 0.124586362215, 0.116548532394, 0.132624192035, 0.078368840748, 0.0843972131132, 0.0582742661972, 0.0763593832929, 0.100472872754, 0.052245893832, 0.0562648087421, 0.0462175214668, 0.0321513192812, 0.0421986065566, 0.026122946916, 0.0321513192812, 0.0140662021855, 0.0120567447305, 0.0241134894609, 0.0140662021855, 0.0160756596406, 0.0140662021855, 0.00803782982031, 0.00602837236523, 0.0120567447305, 0.00803782982031, 0.00803782982031, 0.00602837236523, 0.0100472872754, 0.00200945745508 };
    double mean = 0.951868470698;
    double stddev = 0.571932920001;
    double[] s = new double[testx.length];
    for(int i = 0; i < testx.length; i++) {
      s[i] = 1.0;
    }
    double[] params = { mean, stddev, 1 };
    boolean[] dofit = { true, true, false };
    LevenbergMarquardtMethod fit = new LevenbergMarquardtMethod(GaussianFittingFunction.STATIC, params, dofit, testx, testy, s);
    for(int i = 0; i < 50; i++) {
      fit.iterate();
    }
    double[] ps = fit.getParams();
    // compare results.
    double[] should = { 0.132165, 1.027699, 1 };
    assertEquals("Mean doesn't match.", should[0], ps[0], 0.0001);
    assertEquals("Stddev doesn't match.", should[1], ps[1], 0.0001);
    assertEquals("Scaling doesn't match.", should[2], ps[2], 0.0001);
  }
}