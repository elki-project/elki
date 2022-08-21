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
 * Unit test for the Uniform distribution in ELKI.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class UniformDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("unif.ascii.gz");
    assertPDF(new UniformDistribution(0., 1.), "pdf_gnur_0_1", 1e-15);
    assertPDF(new UniformDistribution(-1., 2.), "pdf_gnur_M1_2", 1e-15);
    assertPDF(new UniformDistribution(0., 1.), "pdf_scipy_0_1", 1e-15);
    assertPDF(new UniformDistribution(-1., 2.), "pdf_scipy_M1_2", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("unif.ascii.gz");
    assertLogPDF(new UniformDistribution(0., 1.), "logpdf_gnur_0_1", 1e-15);
    assertLogPDF(new UniformDistribution(-1., 2.), "logpdf_gnur_M1_2", 1e-15);
    assertLogPDF(new UniformDistribution(0., 1.), "logpdf_scipy_0_1", 1e-15);
    assertLogPDF(new UniformDistribution(-1., 2.), "logpdf_scipy_M1_2", 1e-15);
  }

  @Test
  public void testCDF() {
    load("unif.ascii.gz");
    assertCDF(new UniformDistribution(0., 1.), "cdf_gnur_0_1", 1e-15);
    assertCDF(new UniformDistribution(-1., 2.), "cdf_gnur_M1_2", 1e-15);
    assertCDF(new UniformDistribution(0., 1.), "cdf_scipy_0_1", 1e-15);
    assertCDF(new UniformDistribution(-1., 2.), "cdf_scipy_M1_2", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("unif.ascii.gz");
    assertQuantile(new UniformDistribution(0., 1.), "quant_gnur_0_1", 1e-15);
    assertQuantile(new UniformDistribution(-1., 2.), "quant_gnur_M1_2", 1e-15);
    assertQuantile(new UniformDistribution(0., 1.), "quant_scipy_0_1", 1e-15);
    assertQuantile(new UniformDistribution(-1., 2.), "quant_scipy_M1_2", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("unif.ascii.gz");
    Distribution dist = new ELKIBuilder<>(UniformDistribution.class) //
        .with(UniformDistribution.Par.MIN_ID, 0.) //
        .with(UniformDistribution.Par.MAX_ID, 1.).build();
    assertPDF(dist, "pdf_scipy_0_1", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new UniformDistribution(-1, 2), new Random(0L), 10000, 1e-2);
    assertRandom(new UniformDistribution(1, 4), new Random(1L), 10000, 1e-2);
  }
}
