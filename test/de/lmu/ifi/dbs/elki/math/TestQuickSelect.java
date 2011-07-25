package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.math.statistics.QuickSelect;

/**
 * Test the QuickSelect math class.
 * 
 * @author Erich Schubert
 */
public class TestQuickSelect implements JUnit4Test {
  /**
   * Array size to use.
   */
  final int SIZE = 10000;

  @Test
  public void testRandomDoubles() {
    testQuickSelect(SIZE);
    testQuickSelect(SIZE + 1);
  }

  private void testQuickSelect(int size) {
    double[] data = new double[size];
    double[] test;

    // Make a random generator, but remember the seed for debugging.
    Random r = new Random();
    long seed = r.nextLong();
    r = new Random(seed);

    // Produce data
    for(int i = 0; i < size; i++) {
      data[i] = r.nextDouble();
    }
    // Duplicate for reference, sort.
    test = Arrays.copyOf(data, size);
    Arrays.sort(test);

    // Run QuickSelect and compare with full sort
    // After a few iterations, the array will be largely sorted.
    // Better just run the whole test a few times, than doing too many
    // iterations here.
    for(int j = 0; j < 20; j++) {
      int q = r.nextInt(size);
      double r1 = QuickSelect.quickSelect(data, q);
      double r2 = test[q];
      assertEquals("QuickSelect returned incorrect element. Seed=" + seed, r2, r1, Double.MIN_VALUE);
    }
    double med = QuickSelect.median(data);
    if(size % 2 == 1) {
      double met = test[(size - 1) / 2];
      assertEquals("QuickSelect returned incorrect median. Seed=" + seed, met, med, Double.MIN_VALUE);
    }
    else {
      double met = (test[(size - 1) / 2] + test[(size + 1) / 2]) / 2;
      assertEquals("QuickSelect returned incorrect median. Seed=" + seed, med, met, Double.MIN_VALUE);
    }
    double qua = QuickSelect.quantile(data, 0.5);
    assertEquals("Median and 0.5 quantile do not agree. Seed=" + seed, med, qua, 1E-15);
  }
}
