/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
 * @since 0.4.0
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
}
