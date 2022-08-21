/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.dependence;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import elki.math.statistics.dependence.mcde.MWPTest;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Unit test for MCDE dependence measure.
 * 
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 * @since 0.8.0
 */
public class MCDEDependenceTest {
  @Test
  public void testIndependent() {
    Random generator = new Random(0);
    double[] indi1 = new double[1000], indi2 = new double[1000];
    for(int i = 0; i < 1000; i++) {
      indi1[i] = generator.nextDouble();
      indi2[i] = generator.nextDouble();
    }
    doTest(indi1, indi2, 0.65, 0.35, 0.67, 0.33);
  }

  @Test
  public void testLinear() {
    double[] lin1 = new double[1000], lin2 = new double[1000];
    for(int i = 0; i < 1000; i++) {
      lin1[i] = i;
      lin2[i] = i * 2;
    }
    doTest(lin1, lin2, 1.0, 0.99, 1.0, 0.97);
  }

  @Test
  public void testSmallNoise() {
    Random generator = new Random(0);
    double[] s1 = new double[1000], s2 = new double[1000];
    for(int i = 0; i < 1000; i++) {
      s1[i] = i;
      s2[i] = (i * 2) + generator.nextGaussian() * 100;
    }
    doTest(s1, s2, 1.0, 0.96, 1.0, 0.85);
  }

  @Test
  public void testMoreNoise() {
    Random generator = new Random(0);
    double[] m1 = new double[1000], m2 = new double[1000];
    for(int i = 0; i < 1000; i++) {
      m1[i] = i;
      m2[i] = (i * 2) + generator.nextGaussian() * 500;
    }
    doTest(m1, m2, 1.0, 0.85, 1.0, 0.83);
  }

  private static void doTest(double[] data1, double[] data2, double strict_upper, double strict_lower, double lax_upper, double lax_lower) {
    MCDEDependence mwp = new ELKIBuilder<>(MCDEDependence.class) //
        .with(MCDEDependence.Par.M_ID, 1000) //
        .with(MCDEDependence.Par.TEST_ID, MWPTest.STATIC) //
        .build();
    DoubleArrayAdapter adapter = DoubleArrayAdapter.STATIC;
    double total_res = 0;
    for(int i = 0; i < 100; i++) {
      double res = mwp.dependence(adapter, data1, adapter, data2);
      total_res += res;
      assertTrue("MWP result out of acceptable range. Result: " + res + ". Lax upper bound: " + lax_upper, (res <= lax_upper));
      assertTrue("MWP result out of acceptable range. Result: " + res + ". Lax lower bound: " + lax_lower, (res >= lax_lower));
    }
    total_res /= 100;
    assertTrue("MWP result out of acceptable range. Result: " + total_res + ". Strict upper bound: " + strict_upper, (total_res <= strict_upper));
    assertTrue("MWP result out of acceptable range. Result: " + total_res + ". Strict lower bound: " + strict_lower, (total_res >= strict_lower));
  }
}
