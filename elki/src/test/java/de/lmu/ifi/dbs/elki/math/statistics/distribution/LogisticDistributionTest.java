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
 * Unit test for the Logistic distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class LogisticDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("logistic.ascii.gz");
    checkPDF(new LogisticDistribution(.1, 1.), "pdf_scipy_01", 1e-15);
    checkPDF(new LogisticDistribution(.5, 1.), "pdf_scipy_05", 1e-15);
    checkPDF(new LogisticDistribution(.1, 1.), "pdf_gnur_01", 1e-15);
    checkPDF(new LogisticDistribution(.5, 1.), "pdf_gnur_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("logistic.ascii.gz");
    checkLogPDF(new LogisticDistribution(.1, 1.), "logpdf_scipy_01", 1e-15);
    checkLogPDF(new LogisticDistribution(.5, 1.), "logpdf_scipy_05", 1e-15);
    checkLogPDF(new LogisticDistribution(.1, 1.), "logpdf_gnur_01", 1e-15);
    checkLogPDF(new LogisticDistribution(.5, 1.), "logpdf_gnur_05", 1e-15);
  }

  @Test
  public void testCDF() {
    load("logistic.ascii.gz");
    checkCDF(new LogisticDistribution(.1, 1.), "cdf_scipy_01", 1e-15);
    checkCDF(new LogisticDistribution(.5, 1.), "cdf_scipy_05", 1e-15);
    checkCDF(new LogisticDistribution(.1, 1.), "cdf_gnur_01", 1e-15);
    checkCDF(new LogisticDistribution(.5, 1.), "cdf_gnur_05", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("logistic.ascii.gz");
    checkQuantile(new LogisticDistribution(.1, 1.), "quant_scipy_01", 1e-13);
    checkQuantile(new LogisticDistribution(.5, 1.), "quant_scipy_05", 1e-13);
    checkQuantile(new LogisticDistribution(.1, 1.), "quant_gnur_01", 1e-15);
    checkQuantile(new LogisticDistribution(.5, 1.), "quant_gnur_05", 1e-15);
  }
}
