package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Abstract base class for distribution unit testing.
 * 
 * @author Erich Schubert
 */
public class AbstractDistributionTest {
  public void checkPDF(Distribution d, double[] x, double[] expected, double err) {
    int maxerrlev = -15;
    for(int i = 0; i < x.length; i++) {
      double val = d.pdf(x[i]);
      if(val == expected[i]) {
        continue;
      }
      double diff = Math.abs(val - expected[i]);
      final int errlev = (int) Math.ceil(Math.log10(diff / expected[i]));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / expected[i] < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x[i], expected[i], val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }

  public void checkCDF(Distribution d, double[] x, double[] expected, double err) {
    int maxerrlev = -15;
    for(int i = 0; i < x.length; i++) {
      double val = d.cdf(x[i]);
      if(val == expected[i]) {
        continue;
      }
      double diff = Math.abs(val - expected[i]);
      final int errlev = (int) Math.ceil(Math.log10(diff / expected[i]));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / expected[i] < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x[i], expected[i], val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }

  public void checkQuantile(Distribution d, double[] x, double[] expected, double err) {
    int maxerrlev = -15;
    for(int i = 0; i < x.length; i++) {
      if(Double.isNaN(expected[i])) {
        continue;
      }
      double val = d.quantile(x[i]);
      if(val == expected[i]) {
        continue;
      }
      double diff = Math.abs(val - expected[i]);
      final int errlev = (int) Math.ceil(Math.log10(diff / expected[i]));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / expected[i] < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x[i], expected[i], val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }
}