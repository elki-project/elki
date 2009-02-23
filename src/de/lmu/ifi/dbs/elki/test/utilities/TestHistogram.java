package de.lmu.ifi.dbs.elki.test.utilities;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.Histogram;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

public class TestHistogram {
  Histogram<Double> hist;

  @Test
  public final void testHistogram() {
    Double[] initial = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    Double[] filled = { 0.0, 1.23, 4.56, 7.89, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    Double[] changed = { 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0.0, 0.0, 0.0, 0.0 };
    Double[] resized = { -1.23, 0.0, 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0.0, 0.0, 0.0, 0.0, 0.0, -4.56 };
    hist = Histogram.DoubleHistogram(10, 0.0, 1.0);
    assertArrayEquals("Empty histogram doesn't match", initial, hist.getData().toArray(new Double[0]));
    hist.put(0.15, 1.23);
    hist.put(0.25, 4.56);
    hist.put(0.35, 7.89);
    assertArrayEquals("Filled histogram doesn't match", filled, hist.getData().toArray(new Double[0]));
    hist.put(0.15, 0.12 + hist.get(0.15));
    hist.put(0.25, 3.45 + hist.get(0.25));
    hist.put(0.35, 6.78 + hist.get(0.35));
    hist.put(0.45, 9.01 + hist.get(0.45));
    hist.put(0.50, 2.34 + hist.get(0.50));
    assertArrayEquals("Changed histogram doesn't match", changed, hist.getData().toArray(new Double[0]));
    hist.put(-.13, -1.23 + hist.get(-.13));
    hist.put(1.13, -4.56 + hist.get(1.13));
    assertArrayEquals("Resized histogram doesn't match", resized, hist.getData().toArray(new Double[0]));
    
    // compare results via Iterator.
    int off = 0;
    for (SimplePair<Double, Double> pair : hist) {
      assertEquals("Array iterator bin position", -0.15 + 0.1 * off, pair.getFirst(), 0.00001);
      assertEquals("Array iterator bin contents", resized[off], pair.getSecond(), 0.00001);
      off++;
    }
  }
}
