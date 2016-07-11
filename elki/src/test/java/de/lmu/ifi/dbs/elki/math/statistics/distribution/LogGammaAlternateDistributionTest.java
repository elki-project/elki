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

import org.junit.Test;

/**
 * Unit test for the LogGamma distribution in ELKI.
 * 
 * The reference values were computed using SciPy
 * 
 * FIXME: some values do not make sense.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class LogGammaAlternateDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("loggamma.ascii.gz");
    checkPDF(new LogGammaAlternateDistribution(1., 1., 0.), "pdf_scipy_1_1", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(2., 1., 0.), "pdf_scipy_2_1", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(4., 1., 0.), "pdf_scipy_4_1", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(4., 10, 0.), "pdf_scipy_4_10", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(.1, 10, 0.), "pdf_scipy_01_10", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(.1, 20, 0.), "pdf_scipy_01_20", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(.1, 4., 0.), "pdf_scipy_01_4", 1e-15);
    checkPDF(new LogGammaAlternateDistribution(.1, 1., 0.), "pdf_scipy_01_1", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("loggamma.ascii.gz");
    checkLogPDF(new LogGammaAlternateDistribution(1., 1., 0.), "logpdf_scipy_1_1", 1e-15);
    checkLogPDF(new LogGammaAlternateDistribution(2., 1., 0.), "logpdf_scipy_2_1", 1e-15);
    checkLogPDF(new LogGammaAlternateDistribution(4., 1., 0.), "logpdf_scipy_4_1", 1e-15);
    // For the following, SciPy appears to lose numerical precision and returns inf values:
    // checkLogPDF(new LogGammaAlternateDistribution(4., 10, 0.), "logpdf_scipy_4_10", 1e-14);
    // checkLogPDF(new LogGammaAlternateDistribution(.1, 10, 0.), "logpdf_scipy_01_10", 1e-15);
    // checkLogPDF(new LogGammaAlternateDistribution(.1, 20, 0.), "logpdf_scipy_01_20", 1e-14);
    // checkLogPDF(new LogGammaAlternateDistribution(.1, 4., 0.), "logpdf_scipy_01_4", 1e-15);
    checkLogPDF(new LogGammaAlternateDistribution(.1, 1., 0.), "logpdf_scipy_01_1", 1e-15);
  }

  @Test
  public void testCDF() {
    load("loggamma.ascii.gz");
    checkCDF(new LogGammaAlternateDistribution(1., 1., 0.), "cdf_scipy_1_1", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(2., 1., 0.), "cdf_scipy_2_1", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(4., 1., 0.), "cdf_scipy_4_1", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(4., 10, 0.), "cdf_scipy_4_10", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(.1, 10, 0.), "cdf_scipy_01_10", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(.1, 20, 0.), "cdf_scipy_01_20", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(.1, 4., 0.), "cdf_scipy_01_4", 1e-15);
    checkCDF(new LogGammaAlternateDistribution(.1, 1., 0.), "cdf_scipy_01_1", 1e-15);
  }
}
