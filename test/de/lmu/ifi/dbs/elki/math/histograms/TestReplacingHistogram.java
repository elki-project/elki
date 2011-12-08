package de.lmu.ifi.dbs.elki.math.histograms;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * JUnit test to test the {@link ReplacingHistogram} class.
 * 
 * @author Erich Schubert
 */
public class TestReplacingHistogram implements JUnit4Test {
  ReplacingHistogram<Double> hist;

  /**
   * Test that adds some data to the histogram and compares results.
   */
  @Test
  public final void testHistogram() {
    Double[] initial = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    Double[] filled = { 0.0, 1.23, 4.56, 7.89, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    Double[] changed = { 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0.0, 0.0, 0.0, 0.0 };
    Double[] resized = { -1.23, 0.0, 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0.0, 0.0, 0.0, 0.0, 0.0, -4.56 };
    hist = ReplacingHistogram.DoubleHistogram(10, 0.0, 1.0);
    assertArrayEquals("Empty histogram doesn't match", initial, hist.getData().toArray(new Double[0]));
    hist.replace(0.15, 1.23);
    hist.replace(0.25, 4.56);
    hist.replace(0.35, 7.89);
    assertArrayEquals("Filled histogram doesn't match", filled, hist.getData().toArray(new Double[0]));
    hist.replace(0.15, 0.12 + hist.get(0.15));
    hist.replace(0.25, 3.45 + hist.get(0.25));
    hist.replace(0.35, 6.78 + hist.get(0.35));
    hist.replace(0.45, 9.01 + hist.get(0.45));
    hist.replace(0.50, 2.34 + hist.get(0.50));
    assertArrayEquals("Changed histogram doesn't match", changed, hist.getData().toArray(new Double[0]));
    hist.replace(-.13, -1.23 + hist.get(-.13));
    hist.replace(1.13, -4.56 + hist.get(1.13));
    assertArrayEquals("Resized histogram doesn't match", resized, hist.getData().toArray(new Double[0]));

    // compare results via Iterator.
    int off = 0;
    for(DoubleObjPair<Double> pair : hist) {
      assertEquals("Array iterator bin position", -0.15 + 0.1 * off, pair.first, 0.00001);
      assertEquals("Array iterator bin contents", resized[off], pair.getSecond(), 0.00001);
      off++;
    }
  }
}
