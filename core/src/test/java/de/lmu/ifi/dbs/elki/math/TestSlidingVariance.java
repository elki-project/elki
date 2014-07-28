package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Unit test {@link MeanVariance} with negative weights.
 * 
 * @author Erich Schubert
 */
public class TestSlidingVariance implements JUnit4Test {
  /**
   * Size of test data set.
   */
  private static final int SIZE = 100000;

  /**
   * Sliding window size.
   */
  private static final int WINDOWSIZE = 100;

  @Test
  public void testSlidingWindowVariance() {
    MeanVariance mv = new MeanVariance();
    MeanVariance mc = new MeanVariance();

    Random r = new Random(0);
    double[] data = new double[SIZE];
    for(int i = 0; i < data.length; i++) {
      data[i] = r.nextDouble();
    }
    // Arrays.sort(data);

    // Pre-roll:
    for(int i = 0; i < WINDOWSIZE; i++) {
      mv.put(data[i]);
    }
    // Compare to window approach
    for(int i = WINDOWSIZE; i < data.length; i++) {
      mv.put(data[i - WINDOWSIZE], -1.); // Remove
      mv.put(data[i]);

      mc.reset(); // Reset statistics
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        mc.put(data[j]);
      }
      // Fully manual statistics, exact two-pass algorithm:
      double mean = 0.0;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        mean += data[j];
      }
      mean /= WINDOWSIZE;
      double var = 0.0;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        double v = data[j] - mean;
        var += v * v;
      }
      var /= (WINDOWSIZE - 1);
      assertEquals("Variance does not agree at i=" + i, mv.getSampleVariance(), mc.getSampleVariance(), 1e-14);
      assertEquals("Variance does not agree at i=" + i, mv.getSampleVariance(), var, 1e-14);
    }
  }
}
