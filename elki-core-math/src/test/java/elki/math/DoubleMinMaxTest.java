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
package elki.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * Test the DoubleMinMax class.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class DoubleMinMaxTest {
  /**
   * Note: this test tests an earlier bug with tiny arrays. Keep.
   */
  @Test
  public void basic() {
    DoubleMinMax m = new DoubleMinMax();
    m.put(0);
    m.put(new double[] {});
    m.put(new double[] { 0 });
    m.put(new double[] { -1, +2 });
    m.put(new double[] { 0, 0, 0 });
    assertEquals("Min wrong.", -1, m.getMin(), 0.);
    assertEquals("Max wrong.", +2, m.getMax(), 0.);
    assertEquals("Diff wrong.", 3, m.getDiff(), 0.);
    m.reset();
    assertFalse(m.isValid());
    m.put(0.);
    m.put(new DoubleMinMax(-1, 2));
    assertEquals("Min wrong.", -1, m.getMin(), 0.);
    assertEquals("Max wrong.", +2, m.getMax(), 0.);
    m.put(new DoubleMinMax(-.5, 1));
    assertEquals("Min wrong.", -1, m.getMin(), 0.);
    assertEquals("Max wrong.", +2, m.getMax(), 0.);
    double[] a = m.asDoubleArray();
    assertEquals("Min wrong.", -1, a[0], 0.);
    assertEquals("Max wrong.", +2, a[1], 0.);
  }
}
