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
package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.io.Tokenizer;

/**
 * Abstract base class for distribution unit testing.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AbstractDistributionTest {
  HashMap<String, double[]> data;

  protected void load(String name) {
    data = new HashMap<>();
    try (
        InputStream in = new GZIPInputStream(getClass().getResourceAsStream(name)); //
        TokenizedReader reader = new TokenizedReader(Pattern.compile(" "), "\"", Pattern.compile("^\\s*#.*"))) {
      Tokenizer t = reader.getTokenizer();
      DoubleArray buf = new DoubleArray();
      reader.reset(in);
      while(reader.nextLineExceptComments()) {
        assertTrue(t.valid());
        String key = t.getStrippedSubstring();
        buf.clear();
        for(t.advance(); t.valid(); t.advance()) {
          buf.add(t.getDouble());
        }
        data.put(key, buf.toArray());
      }
    }
    catch(IOException e) {
      fail("Cannot load data.");
    }
  }

  public void checkPDF(Distribution d, String key, double err) {
    // Also check that toString is meaningful.
    assertEquals("No meaningful toString()", -1, d.toString().indexOf('@'));
    double[] data = this.data.get(key);
    assertNotNull("Key not in test data: " + key, data);
    assertEquals("Not NaN for NaN", Double.NaN, d.pdf(Double.NaN), 0.);
    assertEquals("Not zero at neginf", 0., d.pdf(Double.NEGATIVE_INFINITY), 0.);
    assertEquals("Not zero at almost neginf", 0., d.pdf(-Double.MAX_VALUE), 0.);
    assertEquals("Not zero at posinf", 0., d.pdf(Double.POSITIVE_INFINITY), 0.);
    assertEquals("Not zero at almost posinf", 0., d.pdf(Double.MAX_VALUE), 0.);
    int maxerrlev = -15;
    for(int i = 0; i < data.length;) {
      double x = data[i++], exp = data[i++];
      double val = d.pdf(x);
      if(val == exp) {
        continue;
      }
      double diff = Math.abs(val - exp);
      final int errlev = (int) Math.ceil(Math.log10(diff / exp));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / exp < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x, exp, val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }

  public void checkLogPDF(Distribution d, String key, double err) {
    double[] data = this.data.get(key);
    assertNotNull("Key not in test data: " + key, data);
    assertEquals("Not NaN for NaN", Double.NaN, d.logpdf(Double.NaN), 0.);
    assertTrue("Not neginf at neginf: " + d.logpdf(Double.NEGATIVE_INFINITY), d.logpdf(Double.NEGATIVE_INFINITY) < -1e306);
    assertTrue("Not almost neginf at almost neginf: " + d.logpdf(-Double.MAX_VALUE), d.logpdf(-Double.MAX_VALUE) < -1024);
    assertTrue("Not neginf at posinf: " + d.logpdf(Double.POSITIVE_INFINITY), d.logpdf(Double.POSITIVE_INFINITY) < -1e306);
    assertTrue("Not almost neginf at almost posinf: " + d.logpdf(Double.MAX_VALUE), d.logpdf(Double.MAX_VALUE) < -1024);
    int maxerrlev = -15;
    for(int i = 0; i < data.length;) {
      double x = data[i++], exp = data[i++];
      double val = d.logpdf(x);
      if(val == exp) {
        continue;
      }
      double diff = Math.abs(val - exp);
      final int errlev = (int) Math.ceil(Math.log10(diff / exp));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / exp < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x, exp, val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }

  public void checkCDF(Distribution d, String key, double err) {
    double[] data = this.data.get(key);
    assertNotNull("Key not in test data: " + key, data);
    assertEquals("Not NaN for NaN", Double.NaN, d.cdf(Double.NaN), 0.);
    assertEquals("Not zero at neginf", 0., d.cdf(Double.NEGATIVE_INFINITY), 0.);
    assertEquals("Not zero at almost neginf", 0., d.cdf(-Double.MAX_VALUE), 0.);
    assertEquals("Not one at posinf", 1., d.cdf(Double.POSITIVE_INFINITY), 0.);
    assertEquals("Not one at almost posinf", 1., d.cdf(Double.MAX_VALUE), 0.);
    int maxerrlev = -15;
    for(int i = 0; i < data.length;) {
      double x = data[i++], exp = data[i++];
      double val = d.cdf(x);
      if(val == exp) {
        continue;
      }
      double diff = Math.abs(val - exp);
      final int errlev = (int) Math.ceil(Math.log10(diff / exp));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / exp < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x, exp, val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }

  public void checkQuantile(Distribution d, String key, double err) {
    double[] data = this.data.get(key);
    assertEquals("Not NaN for NaN", Double.NaN, d.quantile(Double.NaN), 0.);
    assertEquals("Not NaN for -inf", Double.NaN, d.quantile(Double.NEGATIVE_INFINITY), 0.);
    assertEquals("Not NaN for -1", Double.NaN, d.quantile(-1), 0.);
    assertEquals("Not NaN for 2", Double.NaN, d.quantile(2), 0.);
    assertEquals("Not NaN for inf", Double.NaN, d.quantile(Double.POSITIVE_INFINITY), 0.);
    assertNotNull("Key not in test data: " + key, data);
    int maxerrlev = -15;
    for(int i = 0; i < data.length;) {
      double x = data[i++], exp = data[i++];
      if(Double.isNaN(exp)) {
        continue;
      }
      double val = d.quantile(x);
      if(val == exp) {
        continue;
      }
      double diff = Math.abs(val - exp);
      final int errlev = (int) Math.ceil(Math.log10(diff / exp));
      maxerrlev = Math.max(errlev, maxerrlev);
      if(diff < err || diff / exp < err) {
        continue;
      }
      assertEquals("Error magnitude: 1e" + errlev + " at " + x, exp, val, err);
    }
    int given = (int) Math.floor(Math.log10(err * 1.1));
    assertTrue("Error magnitude is not tight: measured " + maxerrlev + " specified " + given, given <= maxerrlev);
  }

  public void checkRandom(Distribution d, int size, double err) {
    double[] data = new double[size];
    for(int i = 0; i < size; i++) {
      data[i] = d.nextRandom();
    }
    Arrays.sort(data);
    final double q0 = d.cdf(data[0]);
    final double q50 = d.cdf(data[size >> 1]);
    final double q100 = d.cdf(data[size - 1]);
    assertEquals("0% quantile not as expected.", .5 / size, q0, err);
    assertEquals("50% quantile not as expected.", 0.5, q50, err);
    assertEquals("100% quantile not as expected.", 1. - .5 / size, q100, err);
    int errlev0 = (int) Math.ceil(Math.log10(Math.abs(.5 / size - q0)));
    int errlev50 = (int) Math.ceil(Math.log10(Math.abs(0.5 - q50)));
    int errlev100 = (int) Math.ceil(Math.log10(Math.abs(1 - .5 / size - q100)));
    int errlev = MathUtil.max(errlev0, errlev50, errlev100);
    assertEquals("Error magnitude is not tight", errlev, Math.log10(err), 0.1);
  }
}
