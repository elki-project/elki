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
package elki.math.statistics.tests;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import elki.math.linearalgebra.VMath;

/**
 * Unit test to validate the KS-Test
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class KolmogorovSmirnovTestTest {
  @Test
  public void basic() {
    int size = 1000;
    Random r = new Random(0L);
    double[][] a = new double[4][size];
    for(int j = 0; j < a.length; j++) {
      for(int i = 0; i < size; i++) {
        a[j][i] = r.nextDouble();
      }
    }
    for(int j = 0; j < a.length; j++) {
      assertEquals("KS-test identical", 0., KolmogorovSmirnovTest.STATIC.deviation(a[0], a[0]), 0);
    }
    // Random fluctuations in iid data.
    assertEquals("KS-test i.i.d.", .039, KolmogorovSmirnovTest.STATIC.deviation(a[0], a[1]), 1e-3);
    assertEquals("KS-test i.i.d.", .029, KolmogorovSmirnovTest.STATIC.deviation(a[0], a[2]), 1e-3);
    assertEquals("KS-test i.i.d.", .033, KolmogorovSmirnovTest.STATIC.deviation(a[0], a[3]), 1e-3);
    // Now the values become much larger if we increate the offsets
    VMath.plusEquals(a[1], .01);
    assertEquals("KS-test +.01", .047, KolmogorovSmirnovTest.STATIC.deviation(a[0], a[1]), 1e-3);
    VMath.plusEquals(a[2], .1);
    assertEquals("KS-test +.1", .134, KolmogorovSmirnovTest.STATIC.deviation(a[0], a[2]), 1e-3);
    VMath.plusEquals(a[3], .5);
    assertEquals("KS-test +.5", .540, KolmogorovSmirnovTest.STATIC.deviation(a[0], a[3]), 1e-3);
  }
}
