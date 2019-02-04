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
package de.lmu.ifi.dbs.elki.math.scales;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class to test rounding of the linear scale.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 */
public class LinearScaleTest {

  /**
   * Produces a simple linear scale and verifies the tick lines are placed as
   * expected.
   */
  @Test
  public final void testLinearScale() {
    LinearScale a = new LinearScale(3, 97);
    assertEquals("Minimum for scale 3-97 not as expected.", 0.0, a.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale 3-97 not as expected.", 100.0, a.getMax(), Double.MIN_VALUE);

    LinearScale b = new LinearScale(-97, -3);
    assertEquals("Minimum for scale -97 : -3 not as expected.", -100.0, b.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale -97 : -3 not as expected.", 0.0, b.getMax(), Double.MIN_VALUE);

    LinearScale c = new LinearScale(-3, 37);
    assertEquals("Minimum for scale -3 : 37 not as expected.", -10.0, c.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale -3 : 37 not as expected.", 40.0, c.getMax(), Double.MIN_VALUE);

    LinearScale d = new LinearScale(-37, 3);
    assertEquals("Minimum for scale -37 : 3 not as expected.", -40.0, d.getMin(), Double.MIN_VALUE);
    assertEquals("Maximum for scale -37 : 3 not as expected.", 10.0, d.getMax(), Double.MIN_VALUE);
  }

}
