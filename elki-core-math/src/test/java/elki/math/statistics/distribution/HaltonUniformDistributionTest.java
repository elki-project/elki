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
 * Unit test for the Halton pseudo-Uniform distribution in ELKI.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HaltonUniformDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("unif.ascii.gz");
    assertPDF(new HaltonUniformDistribution(0., 1., 3, 0), "pdf_gnur_0_1", 1e-15);
    assertPDF(new HaltonUniformDistribution(-1., 2., 3, 0), "pdf_gnur_M1_2", 1e-15);
    assertPDF(new HaltonUniformDistribution(0., 1., 3, 0), "pdf_scipy_0_1", 1e-15);
    assertPDF(new HaltonUniformDistribution(-1., 2., 3, 0), "pdf_scipy_M1_2", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("unif.ascii.gz");
    assertLogPDF(new HaltonUniformDistribution(0., 1., 3, 0), "logpdf_gnur_0_1", 1e-15);
    assertLogPDF(new HaltonUniformDistribution(-1., 2., 3, 0), "logpdf_gnur_M1_2", 1e-15);
    assertLogPDF(new HaltonUniformDistribution(0., 1., 3, 0), "logpdf_scipy_0_1", 1e-15);
    assertLogPDF(new HaltonUniformDistribution(-1., 2., 3, 0), "logpdf_scipy_M1_2", 1e-15);
  }

  @Test
  public void testCDF() {
    load("unif.ascii.gz");
    assertCDF(new HaltonUniformDistribution(0., 1., 3, 0), "cdf_gnur_0_1", 1e-15);
    assertCDF(new HaltonUniformDistribution(-1., 2., 3, 0), "cdf_gnur_M1_2", 1e-15);
    assertCDF(new HaltonUniformDistribution(0., 1., 3, 0), "cdf_scipy_0_1", 1e-15);
    assertCDF(new HaltonUniformDistribution(-1., 2., 3, 0), "cdf_scipy_M1_2", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("unif.ascii.gz");
    assertQuantile(new HaltonUniformDistribution(0., 1., 3, 0), "quant_gnur_0_1", 1e-15);
    assertQuantile(new HaltonUniformDistribution(-1., 2., 3, 0), "quant_gnur_M1_2", 1e-15);
    assertQuantile(new HaltonUniformDistribution(0., 1., 3, 0), "quant_scipy_0_1", 1e-15);
    assertQuantile(new HaltonUniformDistribution(-1., 2., 3, 0), "quant_scipy_M1_2", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("unif.ascii.gz");
    Distribution dist = new ELKIBuilder<>(HaltonUniformDistribution.class) //
        .with(UniformDistribution.Par.MIN_ID, 0.) //
        .with(UniformDistribution.Par.MAX_ID, 1.).build();
    assertPDF(dist, "pdf_scipy_0_1", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new HaltonUniformDistribution(-1, 2, 3, 0.1234), new Random(0L), 10000, 1e-4);
    assertRandom(new HaltonUniformDistribution(1, 4, 3, 0.1234), new Random(1L), 10000, 1e-4);
  }
}
