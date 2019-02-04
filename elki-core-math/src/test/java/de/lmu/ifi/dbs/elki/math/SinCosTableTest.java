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
 * Test the sin-cos lookup table.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SinCosTableTest {
  @Test
  public void testSinCosTable() {
    doSinCosTableTest(360);
    doSinCosTableTest(142); // Divisible by two
    doSinCosTableTest(17);
    doSinCosTableTest(131); // Prime.
  }

  protected void doSinCosTableTest(int steps) {
    SinCosTable table = SinCosTable.make(steps);
    for(int i = -steps; i < 2 * steps; i++) {
      double angle = Math.toRadians(360. * i / steps);
      assertEquals("Cosine does not match at i=" + i + " a=" + angle, Math.cos(angle), table.cos(i), 1E-10);
      assertEquals("Sine does not match at i=" + i + " a=" + angle, Math.sin(angle), table.sin(i), 1E-10);
    }
  }
}
