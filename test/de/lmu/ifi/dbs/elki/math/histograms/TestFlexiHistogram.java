package de.lmu.ifi.dbs.elki.math.histograms;

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * JUnit test to test the {@link ReplacingHistogram} class.
 * 
 * @author Erich Schubert
 */
public class TestFlexiHistogram implements JUnit4Test {
  FlexiHistogram<Double, Double> hist;

  /**
   * Test that adds some data to the histogram and compares results.
   */
  @Test
  public final void testHistogram() {
    Double[] filled = { 0.0, 1.23, 4.56, 7.89, 0.0 };
    Double[] changed = { 0.0, 1.35, 8.01, 14.67, 9.01, 2.34 };
    Double[] resized = { -1.23, 1.35, 22.68, 11.35, 0.0, 0.0, -4.56 };
    Double[] expanded = { 1., 0.0, 0.0, 0.0, 0.0, 0.0, 29.59 };
    hist = FlexiHistogram.DoubleSumHistogram(5);
    hist.aggregate(0.0, 0.0);
    hist.aggregate(0.15, 1.23);
    hist.aggregate(0.25, 4.56);
    hist.aggregate(0.35, 7.89);
    hist.aggregate(0.5, 0.0);
    assertArrayEquals("Filled histogram doesn't match", filled, hist.getData().toArray(new Double[0]));
    hist.aggregate(0.15, 0.12);
    hist.aggregate(0.25, 3.45);
    hist.aggregate(0.35, 6.78);
    hist.aggregate(0.45, 9.01);
    hist.aggregate(0.55, 2.34);
    assertArrayEquals("Changed histogram doesn't match", changed, hist.getData().toArray(new Double[0]));
    hist.aggregate(-.13, -1.23);
    hist.aggregate(1.13, -4.56);
    assertArrayEquals("Resized histogram doesn't match", resized, hist.getData().toArray(new Double[0]));

    // compare results via Iterator.
    int off = 0;
    for(DoubleObjPair<Double> pair : hist) {
      assertEquals("Array iterator bin position", -0.1 + 0.2 * off, pair.first, 0.00001);
      assertEquals("Array iterator bin contents", resized[off], pair.getSecond(), 0.00001);
      off++;
    }
    // backwards...
    off--;
    for(Iterator<DoubleObjPair<Double>> iter = hist.reverseIterator(); iter.hasNext(); ) {
      DoubleObjPair<Double> pair = iter.next();
      assertEquals("Array iterator bin position", -0.1 + 0.2 * off, pair.first, 0.00001);
      assertEquals("Array iterator bin contents", resized[off], pair.getSecond(), 0.00001);
      off--;
    }

    // totally break out of the data range
    hist.aggregate(-10., 1.);
    assertArrayEquals("Expanded histogram doesn't match", expanded, hist.getData().toArray(new Double[0]));
  }
}
