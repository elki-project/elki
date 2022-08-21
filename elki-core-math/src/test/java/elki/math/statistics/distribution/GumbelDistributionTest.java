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
 * Unit test for the Gumbel distribution in ELKI.
 * <p>
 * The reference values were computed using SciPy and GNUR
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class GumbelDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("gumbel.ascii.gz");
    assertPDF(new GumbelDistribution(1., 1.), "pdf_scipy_1_1", 1e-15);
    assertPDF(new GumbelDistribution(2., 1.), "pdf_scipy_2_1", 1e-15);
    assertPDF(new GumbelDistribution(4., 1.), "pdf_scipy_4_1", 1e-15);
    assertPDF(new GumbelDistribution(4., 10.), "pdf_scipy_4_10", 1e-15);
    assertPDF(new GumbelDistribution(.1, 1.), "pdf_scipy_01_1", 1e-15);
    assertPDF(new GumbelDistribution(.1, 4.), "pdf_scipy_01_4", 1e-15);
    assertPDF(new GumbelDistribution(.1, 10.), "pdf_scipy_01_10", 1e-15);
    assertPDF(new GumbelDistribution(.1, 20.), "pdf_scipy_01_20", 1e-15);

    assertPDF(new GumbelDistribution(1., 1.), "pdf_gnur_1_1", 1e-15);
    assertPDF(new GumbelDistribution(2., 1.), "pdf_gnur_2_1", 1e-15);
    assertPDF(new GumbelDistribution(4., 1.), "pdf_gnur_4_1", 1e-15);
    assertPDF(new GumbelDistribution(4., 10.), "pdf_gnur_4_10", 1e-15);
    assertPDF(new GumbelDistribution(.1, 1.), "pdf_gnur_01_1", 1e-15);
    assertPDF(new GumbelDistribution(.1, 4.), "pdf_gnur_01_4", 1e-15);
    assertPDF(new GumbelDistribution(.1, 10.), "pdf_gnur_01_10", 1e-15);
    assertPDF(new GumbelDistribution(.1, 20.), "pdf_gnur_01_20", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("gumbel.ascii.gz");
    assertLogPDF(new GumbelDistribution(1., 1.), "logpdf_scipy_1_1", 1e-15);
    assertLogPDF(new GumbelDistribution(2., 1.), "logpdf_scipy_2_1", 1e-15);
    assertLogPDF(new GumbelDistribution(4., 1.), "logpdf_scipy_4_1", 1e-15);
    assertLogPDF(new GumbelDistribution(4., 10.), "logpdf_scipy_4_10", 1e-15);
    assertLogPDF(new GumbelDistribution(.1, 1.), "logpdf_scipy_01_1", 1e-15);
    assertLogPDF(new GumbelDistribution(.1, 4.), "logpdf_scipy_01_4", 1e-15);
    assertLogPDF(new GumbelDistribution(.1, 10.), "logpdf_scipy_01_10", 1e-15);
    assertLogPDF(new GumbelDistribution(.1, 20.), "logpdf_scipy_01_20", 1e-15);

    // Not in lmomco
  }

  @Test
  public void testCDF() {
    load("gumbel.ascii.gz");
    assertCDF(new GumbelDistribution(1., 1.), "cdf_scipy_1_1", 1e-15);
    assertCDF(new GumbelDistribution(2., 1.), "cdf_scipy_2_1", 1e-15);
    assertCDF(new GumbelDistribution(4., 1.), "cdf_scipy_4_1", 1e-15);
    assertCDF(new GumbelDistribution(4., 10.), "cdf_scipy_4_10", 1e-15);
    assertCDF(new GumbelDistribution(.1, 1.), "cdf_scipy_01_1", 1e-15);
    assertCDF(new GumbelDistribution(.1, 4.), "cdf_scipy_01_4", 1e-15);
    assertCDF(new GumbelDistribution(.1, 10.), "cdf_scipy_01_10", 1e-15);
    assertCDF(new GumbelDistribution(.1, 20.), "cdf_scipy_01_20", 1e-15);

    assertCDF(new GumbelDistribution(1., 1.), "cdf_gnur_1_1", 1e-15);
    assertCDF(new GumbelDistribution(2., 1.), "cdf_gnur_2_1", 1e-15);
    assertCDF(new GumbelDistribution(4., 1.), "cdf_gnur_4_1", 1e-15);
    assertCDF(new GumbelDistribution(4., 10.), "cdf_gnur_4_10", 1e-15);
    assertCDF(new GumbelDistribution(.1, 1.), "cdf_gnur_01_1", 1e-15);
    assertCDF(new GumbelDistribution(.1, 4.), "cdf_gnur_01_4", 1e-15);
    assertCDF(new GumbelDistribution(.1, 10.), "cdf_gnur_01_10", 1e-15);
    assertCDF(new GumbelDistribution(.1, 20.), "cdf_gnur_01_20", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("gumbel.ascii.gz");
    assertQuantile(new GumbelDistribution(1., 1.), "quant_scipy_1_1", 1e-15);
    assertQuantile(new GumbelDistribution(2., 1.), "quant_scipy_2_1", 1e-15);
    assertQuantile(new GumbelDistribution(4., 1.), "quant_scipy_4_1", 1e-15);
    assertQuantile(new GumbelDistribution(4., 10.), "quant_scipy_4_10", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 1.), "quant_scipy_01_1", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 4.), "quant_scipy_01_4", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 10.), "quant_scipy_01_10", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 20.), "quant_scipy_01_20", 1e-15);

    assertQuantile(new GumbelDistribution(1., 1.), "quant_gnur_1_1", 1e-15);
    assertQuantile(new GumbelDistribution(2., 1.), "quant_gnur_2_1", 1e-15);
    assertQuantile(new GumbelDistribution(4., 1.), "quant_gnur_4_1", 1e-15);
    assertQuantile(new GumbelDistribution(4., 10.), "quant_gnur_4_10", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 1.), "quant_gnur_01_1", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 4.), "quant_gnur_01_4", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 10.), "quant_gnur_01_10", 1e-15);
    assertQuantile(new GumbelDistribution(.1, 20.), "quant_gnur_01_20", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("gumbel.ascii.gz");
    Distribution dist = new ELKIBuilder<>(GumbelDistribution.class) //
        .with(GumbelDistribution.Par.LOCATION_ID, 2.) //
        .with(GumbelDistribution.Par.SHAPE_ID, 1.).build();
    assertPDF(dist, "pdf_scipy_2_1", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new GumbelDistribution(0.1, 0.9), new Random(0L), 10000, 1e-2);
    assertRandom(new GumbelDistribution(1.41, 3.14), new Random(0L), 10000, 1e-2);
    assertRandom(new GumbelDistribution(3.14, 1.41), new Random(0L), 10000, 1e-2);
  }
}
