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

/**
 * Test the Mean class.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MeanTest {
  @Test
  public void testInfinity() {
    Mean m = new Mean();
    m.put(Double.POSITIVE_INFINITY);
    m.put(0.);
    assertEquals(2, m.getCount(), 0);
    assertEquals("Sensitive to infinity", Double.POSITIVE_INFINITY, m.getMean(), 0);
    m = new Mean();
    m.put(Double.NEGATIVE_INFINITY);
    m.put(0.);
    assertEquals(2, m.getCount(), 0);
    assertEquals("Sensitive to infinity", Double.NEGATIVE_INFINITY, m.getMean(), 0);
  }

  /**
   * Note: this test tests an earlier bug with tiny arrays. Keep.
   */
  @Test
  public void basic() {
    Mean m = new Mean();
    m.put(0);
    m.put(new double[] {});
    m.put(new double[] { 0 });
    m.put(new double[] { 0, 0 });
    m.put(new double[] { 0, 0, 0 });
    assertEquals("Count wrong.", 7, m.getCount(), 0.);
    assertEquals("Mean wrong.", 0, m.getMean(), 0.);
    assertEquals("No toString", -1, m.toString().indexOf('@'));
    assertEquals("Static helper", 2, Mean.of(1, 2, 3), 0.);
    assertEquals("Static helper", 2, Mean.highPrecision(1, 2, 3), 0.);
  }

  @Test
  public void combine() {
    Mean m1 = new Mean(), m2 = new Mean();
    m1.put(new double[] { 1, 2, 3 });
    m2.put(new double[] { 4, 5, 6, 7 });
    Mean m3 = new Mean(m1);
    m3.put(m2);
    assertEquals("First mean", 2, m1.getMean(), 0.);
    assertEquals("Second mean", 5.5, m2.getMean(), 0.);
    assertEquals("Third mean", 4, m3.getMean(), 0.);
    m2.put(new double[] { 1, 2, 3 }, new double[] { 3, 2, 1 });
    assertEquals("Fourth mean", 3.2, m2.getMean(), 1e-15);
  }
}
