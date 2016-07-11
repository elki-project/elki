package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;

/**
 * Abstract base class for distribution unit testing.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class AbstractDistributionTest {
  protected MultipleObjectsBundle bundle;

  protected void load(String name) {
    try (InputStream res = getClass().getResourceAsStream(name); //
        InputStream data = new BufferedInputStream(new GZIPInputStream(res))) {
      bundle = new InputStreamDatabaseConnection(data, null, //
          new NumberVectorLabelParser<>(DoubleVector.FACTORY)).loadData();
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
    assertTrue(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(bundle.meta(0)));
    assertTrue(TypeUtil.LABELLIST.isAssignableFromType(bundle.meta(1)));
    for(int i = 0; i < bundle.dataLength(); i++) {
      LabelList l = (LabelList) bundle.data(i, 1);
      assertEquals("Multiple labels: " + l.toString(), 1, l.size());
    }
  }

  protected double[] get(String key) {
    for(int i = 0; i < bundle.dataLength(); i++) {
      LabelList labels = (LabelList) bundle.data(i, 1);
      if(key.equals(labels.get(0))) {
        return ((NumberVector) bundle.data(i, 0)).toArray();
      }
    }
    return null;
  }

  public void checkPDF(Distribution d, String key, double err) {
    double[] data = get(key);
    assertTrue("Key not in test data: " + key, data != null);
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
    double[] data = get(key);
    assertTrue("Key not in test data: " + key, data != null);
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
    double[] data = get(key);
    assertTrue("Key not in test data: " + key, data != null);
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
    double[] data = get(key);
    assertTrue("Key not in test data: " + key, data != null);
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
}