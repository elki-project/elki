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
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Unit test for the Halton pseudo-Uniform distribution in ELKI.
 *
 * @author Erich Schubert
 */
public class HaltonUniformDistributionTest extends AbstractDistributionTest implements JUnit4Test {
  public static final double[] P_CDFPDF = { //
      0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 1e-05, 1e-10, 0.1234567, 3.14159265359, 2.71828182846, 0.314159265359, 0.271828182846 //
  };

  public static final double[] P_PROBIT = { //
      0.0001, 0.001, 0.01, 0.1, 0.25, 0.5, 0.75, 0.9, 0.99, 0.999, 0.9999 //
  };

  @Test
  public void testPDF() {
    double[] buf = new double[P_CDFPDF.length];
    for(double l : new double[] { -1, 0, .1, 1 }) {
      for(double h : new double[] { -1, 0, .1, 1, 2, 10 }) {
        if(l < h) {
          double w = h - l;
          for(int i = 0; i < P_CDFPDF.length; i++) {
            buf[i] = (P_CDFPDF[i] >= l && P_CDFPDF[i] < h) ? 1. / w : 0.;
          }
          checkPDF(new HaltonUniformDistribution(l, h), P_CDFPDF, buf, 1e-15);
        }
      }
    }
  }

  @Test
  public void testCDF() {
    double[] buf = new double[P_CDFPDF.length];
    for(double l : new double[] { -1, 0, .1, 1 }) {
      for(double h : new double[] { -1, 0, .1, 1, 2, 10 }) {
        if(l < h) {
          double w = h - l;
          for(int i = 0; i < P_CDFPDF.length; i++) {
            buf[i] = P_CDFPDF[i] <= l ? 0. : P_CDFPDF[i] >= h ? 1. : (P_CDFPDF[i] - l) / w;
          }
          checkCDF(new HaltonUniformDistribution(l, h), P_CDFPDF, buf, 1e-15);
        }
      }
    }
  }

  @Test
  public void testProbit() {
    double[] buf = new double[P_PROBIT.length];
    for(double l : new double[] { -1, 0, .1, 1 }) {
      for(double h : new double[] { -1, 0, .1, 1, 2, 10 }) {
        if(l < h) {
          double w = h - l;
          for(int i = 0; i < P_PROBIT.length; i++) {
            buf[i] = l + P_PROBIT[i] * w;
          }
          checkQuantile(new HaltonUniformDistribution(l, h), P_PROBIT, buf, 1e-15);
        }
      }
    }
  }
}