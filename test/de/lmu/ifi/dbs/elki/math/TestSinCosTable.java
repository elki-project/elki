package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class TestSinCosTable implements JUnit4Test {

  @Test
  public void testSinCosTable() {
    doTestSinCosTable(360);
    doTestSinCosTable(142); // Divisible by two
    doTestSinCosTable(17);
    doTestSinCosTable(131); // Prime.
  }

  protected void doTestSinCosTable(int steps) {
    SinCosTable table = SinCosTable.make(steps);
    for (int i = -steps; i < 2 * steps; i++) {
      double angle = Math.toRadians(360. * i / steps);
      assertEquals("Cosine does not match at i=" + i + " a=" + angle, Math.cos(angle), table.cos(i), 1E-10);
      assertEquals("Sine does not match at i=" + i + " a=" + angle, Math.sin(angle), table.sin(i), 1E-10);
    }
  }
}
