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
 * Unit test for the Chi Squared distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ChiSquaredDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("chisq.ascii.gz");
    checkPDF(new ChiSquaredDistribution(1.), "pdf_scipy_1", 1e-15);
    checkPDF(new ChiSquaredDistribution(2.), "pdf_scipy_2", 1e-15);
    checkPDF(new ChiSquaredDistribution(4.), "pdf_scipy_4", 1e-15);
    checkPDF(new ChiSquaredDistribution(10), "pdf_scipy_10", 1e-15);
    checkPDF(new ChiSquaredDistribution(.1), "pdf_scipy_01", 1e-14);
    checkPDF(new ChiSquaredDistribution(1.), "pdf_gnur_1", 1e-14);
    checkPDF(new ChiSquaredDistribution(2.), "pdf_gnur_2", 1e-15);
    checkPDF(new ChiSquaredDistribution(4.), "pdf_gnur_4", 1e-15);
    checkPDF(new ChiSquaredDistribution(10), "pdf_gnur_10", 1e-15);
    checkPDF(new ChiSquaredDistribution(.1), "pdf_gnur_01", 1e-14);
  }

  @Test
  public void testLogPDF() {
    load("chisq.ascii.gz");
    checkLogPDF(new ChiSquaredDistribution(1.), "logpdf_scipy_1", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(2.), "logpdf_scipy_2", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(4.), "logpdf_scipy_4", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(10), "logpdf_scipy_10", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(.1), "logpdf_scipy_01", 1e-14);

    checkLogPDF(new ChiSquaredDistribution(1.), "logpdf_gnur_1", 1e-14);
    checkLogPDF(new ChiSquaredDistribution(2.), "logpdf_gnur_2", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(4.), "logpdf_gnur_4", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(10), "logpdf_gnur_10", 1e-15);
    checkLogPDF(new ChiSquaredDistribution(.1), "logpdf_gnur_01", 1e-14);
  }

  @Test
  public void testCDF() {
    load("chisq.ascii.gz");
    checkCDF(new ChiSquaredDistribution(1.), "cdf_scipy_1", 1e-14);
    checkCDF(new ChiSquaredDistribution(2.), "cdf_scipy_2", 1e-15);
    checkCDF(new ChiSquaredDistribution(4.), "cdf_scipy_4", 1e-15);
    checkCDF(new ChiSquaredDistribution(10), "cdf_scipy_10", 1e-14);
    checkCDF(new ChiSquaredDistribution(.1), "cdf_scipy_01", 1e-14);

    checkCDF(new ChiSquaredDistribution(1.), "cdf_gnur_1", 1e-15);
    checkCDF(new ChiSquaredDistribution(2.), "cdf_gnur_2", 1e-15);
    checkCDF(new ChiSquaredDistribution(4.), "cdf_gnur_4", 1e-15);
    checkCDF(new ChiSquaredDistribution(10), "cdf_gnur_10", 1e-15);
    checkCDF(new ChiSquaredDistribution(.1), "cdf_gnur_01", 1e-14);
  }

  @Test
  public void testQuantile() {
    load("chisq.ascii.gz");
    checkQuantile(new ChiSquaredDistribution(1.), "quant_scipy_1", 1e-13);
    checkQuantile(new ChiSquaredDistribution(2.), "quant_scipy_2", 1e-13);
    checkQuantile(new ChiSquaredDistribution(4.), "quant_scipy_4", 1e-13);
    checkQuantile(new ChiSquaredDistribution(10), "quant_scipy_10", 1e-12);
    checkQuantile(new ChiSquaredDistribution(.1), "quant_scipy_01", 1e-13);

    checkQuantile(new ChiSquaredDistribution(1.), "quant_gnur_1", 1e-13);
    checkQuantile(new ChiSquaredDistribution(2.), "quant_gnur_2", 1e-14);
    checkQuantile(new ChiSquaredDistribution(4.), "quant_gnur_4", 1e-13);
    checkQuantile(new ChiSquaredDistribution(10), "quant_gnur_10", 1e-13);
    checkQuantile(new ChiSquaredDistribution(.1), "quant_gnur_01", 1e-13);
  }
}