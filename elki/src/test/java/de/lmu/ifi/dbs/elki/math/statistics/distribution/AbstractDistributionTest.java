package de.lmu.ifi.dbs.elki.math.statistics.distribution;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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