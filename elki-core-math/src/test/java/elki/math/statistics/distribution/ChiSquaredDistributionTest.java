/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.distribution;

import java.util.Random;

import org.junit.Test;

import elki.utilities.ELKIBuilder;
import elki.utilities.exceptions.ClassInstantiationException;

/**
 * Unit test for the Chi Squared distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ChiSquaredDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("chisq.ascii.gz");
    assertPDF(new ChiSquaredDistribution(1.), "pdf_scipy_1", 1e-15);
    assertPDF(new ChiSquaredDistribution(2.), "pdf_scipy_2", 1e-15);
    assertPDF(new ChiSquaredDistribution(4.), "pdf_scipy_4", 1e-15);
    assertPDF(new ChiSquaredDistribution(10), "pdf_scipy_10", 1e-15);
    assertPDF(new ChiSquaredDistribution(.1), "pdf_scipy_01", 1e-14);
    assertPDF(new ChiSquaredDistribution(1.), "pdf_gnur_1", 1e-14);
    assertPDF(new ChiSquaredDistribution(2.), "pdf_gnur_2", 1e-15);
    assertPDF(new ChiSquaredDistribution(4.), "pdf_gnur_4", 1e-15);
    assertPDF(new ChiSquaredDistribution(10), "pdf_gnur_10", 1e-15);
    assertPDF(new ChiSquaredDistribution(.1), "pdf_gnur_01", 1e-14);
  }

  @Test
  public void testLogPDF() {
    load("chisq.ascii.gz");
    assertLogPDF(new ChiSquaredDistribution(1.), "logpdf_scipy_1", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(2.), "logpdf_scipy_2", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(4.), "logpdf_scipy_4", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(10), "logpdf_scipy_10", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(.1), "logpdf_scipy_01", 1e-14);

    assertLogPDF(new ChiSquaredDistribution(1.), "logpdf_gnur_1", 1e-14);
    assertLogPDF(new ChiSquaredDistribution(2.), "logpdf_gnur_2", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(4.), "logpdf_gnur_4", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(10), "logpdf_gnur_10", 1e-15);
    assertLogPDF(new ChiSquaredDistribution(.1), "logpdf_gnur_01", 1e-14);
  }

  @Test
  public void testCDF() {
    load("chisq.ascii.gz");
    assertCDF(new ChiSquaredDistribution(1.), "cdf_scipy_1", 1e-14);
    assertCDF(new ChiSquaredDistribution(2.), "cdf_scipy_2", 1e-15);
    assertCDF(new ChiSquaredDistribution(4.), "cdf_scipy_4", 1e-15);
    assertCDF(new ChiSquaredDistribution(10), "cdf_scipy_10", 1e-14);
    assertCDF(new ChiSquaredDistribution(.1), "cdf_scipy_01", 1e-14);

    assertCDF(new ChiSquaredDistribution(1.), "cdf_gnur_1", 1e-15);
    assertCDF(new ChiSquaredDistribution(2.), "cdf_gnur_2", 1e-15);
    assertCDF(new ChiSquaredDistribution(4.), "cdf_gnur_4", 1e-15);
    assertCDF(new ChiSquaredDistribution(10), "cdf_gnur_10", 1e-15);
    assertCDF(new ChiSquaredDistribution(.1), "cdf_gnur_01", 1e-14);
  }

  @Test
  public void testQuantile() {
    load("chisq.ascii.gz");
    assertQuantile(new ChiSquaredDistribution(1.), "quant_scipy_1", 1e-13);
    assertQuantile(new ChiSquaredDistribution(2.), "quant_scipy_2", 1e-13);
    assertQuantile(new ChiSquaredDistribution(4.), "quant_scipy_4", 1e-13);
    assertQuantile(new ChiSquaredDistribution(10), "quant_scipy_10", 1e-12);
    assertQuantile(new ChiSquaredDistribution(.1), "quant_scipy_01", 1e-13);

    assertQuantile(new ChiSquaredDistribution(1.), "quant_gnur_1", 1e-13);
    assertQuantile(new ChiSquaredDistribution(2.), "quant_gnur_2", 1e-14);
    assertQuantile(new ChiSquaredDistribution(4.), "quant_gnur_4", 1e-13);
    assertQuantile(new ChiSquaredDistribution(10), "quant_gnur_10", 1e-13);
    assertQuantile(new ChiSquaredDistribution(.1), "quant_gnur_01", 1e-13);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("chisq.ascii.gz");
    Distribution dist = new ELKIBuilder<>(ChiSquaredDistribution.class) //
        .with(ChiSquaredDistribution.Par.DOF_ID, 2.).build();
    assertPDF(dist, "pdf_scipy_2", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new ChiSquaredDistribution(0.1), new Random(0L), 10000, 1e-2);
    assertRandom(new ChiSquaredDistribution(1.41), new Random(0L), 10000, 1e-2);
    assertRandom(new ChiSquaredDistribution(3.14), new Random(0L), 10000, 1e-2);
  }
}
