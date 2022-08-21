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
 * Unit test for the Normal distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LogNormalDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("lognorm.ascii.gz");
    assertPDF(new LogNormalDistribution(0., 1., 0), "pdf_gnur_0_1", 1e-15);
    assertPDF(new LogNormalDistribution(1., 3., 0), "pdf_gnur_1_3", 1e-15);
    assertPDF(new LogNormalDistribution(.1, .1, 0), "pdf_gnur_01_01", 1e-15);
    assertPDF(new LogNormalDistribution(0., 1., 0), "pdf_scipy_0_1", 1e-14);
    assertPDF(new LogNormalDistribution(1., 3., 0), "pdf_scipy_1_3", 1e-14);
    assertPDF(new LogNormalDistribution(.1, .1, 0), "pdf_scipy_01_01", 1e-14);
  }

  @Test
  public void testLogPDF() {
    load("lognorm.ascii.gz");
    assertLogPDF(new LogNormalDistribution(0., 1., 0), "logpdf_gnur_0_1", 1e-15);
    assertLogPDF(new LogNormalDistribution(1., 3., 0), "logpdf_gnur_1_3", 1e-15);
    assertLogPDF(new LogNormalDistribution(.1, .1, 0), "logpdf_gnur_01_01", 1e-15);
    assertLogPDF(new LogNormalDistribution(0., 1., 0), "logpdf_scipy_0_1", 1e-14);
    assertLogPDF(new LogNormalDistribution(1., 3., 0), "logpdf_scipy_1_3", 1e-14);
    assertLogPDF(new LogNormalDistribution(.1, .1, 0), "logpdf_scipy_01_01", 1e-14);
  }

  @Test
  public void testCDF() {
    load("lognorm.ascii.gz");
    assertCDF(new LogNormalDistribution(0., 1., 0), "cdf_gnur_0_1", 1e-15);
    assertCDF(new LogNormalDistribution(1., 3., 0), "cdf_gnur_1_3", 1e-15);
    assertCDF(new LogNormalDistribution(.1, .1, 0), "cdf_gnur_01_01", 1e-15);
    assertCDF(new LogNormalDistribution(0., 1., 0), "cdf_scipy_0_1", 1e-12);
    assertCDF(new LogNormalDistribution(1., 3., 0), "cdf_scipy_1_3", 1e-12);
    assertCDF(new LogNormalDistribution(.1, .1, 0), "cdf_scipy_01_01", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("lognorm.ascii.gz");
    assertQuantile(new LogNormalDistribution(0., 1., 0), "quant_gnur_0_1", 1e-15);
    assertQuantile(new LogNormalDistribution(1., 3., 0), "quant_gnur_1_3", 1e-14);
    assertQuantile(new LogNormalDistribution(.1, .1, 0), "quant_gnur_01_01", 1e-15);
    assertQuantile(new LogNormalDistribution(0., 1., 0), "quant_scipy_0_1", 1e-15);
    assertQuantile(new LogNormalDistribution(1., 3., 0), "quant_scipy_1_3", 1e-14);
    assertQuantile(new LogNormalDistribution(.1, .1, 0), "quant_scipy_01_01", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("lognorm.ascii.gz");
    Distribution dist = new ELKIBuilder<>(LogNormalDistribution.class) //
        .with(LogNormalDistribution.Par.LOGMEAN_ID, 1.) //
        .with(LogNormalDistribution.Par.LOGSTDDEV_ID, 3.).build();
    assertPDF(dist, "pdf_scipy_1_3", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new LogNormalDistribution(0.1, 0.9, 1), new Random(0L), 10000, 1e-2);
    assertRandom(new LogNormalDistribution(1.41, 3.14, 2), new Random(0L), 10000, 1e-2);
    assertRandom(new LogNormalDistribution(3.14, 1.41, 3), new Random(0L), 10000, 1e-2);
  }
}
