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

import elki.math.MathUtil;
import elki.utilities.ELKIBuilder;
import elki.utilities.exceptions.ClassInstantiationException;

/**
 * Unit test for the Rayleigh distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class RayleighDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("weibull.ascii.gz");
    assertPDF(new RayleighDistribution(MathUtil.SQRTHALF), "pdf_scipy_2_1", 1e-15);
    assertPDF(new RayleighDistribution(MathUtil.SQRTHALF), "pdf_gnur_2_1", 1e-15);
    load("ray.ascii.gz");
    assertPDF(new RayleighDistribution(1), "pdf_scipy_1", 1e-15);
    assertPDF(new RayleighDistribution(2), "pdf_scipy_2", 1e-15);
    assertPDF(new RayleighDistribution(1), "pdf_gnur_1", 1e-15);
    assertPDF(new RayleighDistribution(2), "pdf_gnur_2", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("weibull.ascii.gz");
    assertLogPDF(new RayleighDistribution(MathUtil.SQRTHALF), "logpdf_scipy_2_1", 1e-15);
    assertLogPDF(new RayleighDistribution(MathUtil.SQRTHALF), "logpdf_gnur_2_1", 1e-15);
    load("ray.ascii.gz");
    assertLogPDF(new RayleighDistribution(1), "logpdf_scipy_1", 1e-15);
    assertLogPDF(new RayleighDistribution(2), "logpdf_scipy_2", 1e-15);
    assertLogPDF(new RayleighDistribution(1), "logpdf_gnur_1", 1e-15);
    assertLogPDF(new RayleighDistribution(2), "logpdf_gnur_2", 1e-15);
  }

  @Test
  public void testCDF() {
    load("weibull.ascii.gz");
    assertCDF(new RayleighDistribution(MathUtil.SQRTHALF), "cdf_scipy_2_1", 1e-15);
    assertCDF(new RayleighDistribution(MathUtil.SQRTHALF), "cdf_gnur_2_1", 1e-15);
    load("ray.ascii.gz");
    assertCDF(new RayleighDistribution(1), "cdf_scipy_1", 1e-14);
    assertCDF(new RayleighDistribution(2), "cdf_scipy_2", 1e-14);
    assertCDF(new RayleighDistribution(1), "cdf_gnur_1", 1e-15);
    assertCDF(new RayleighDistribution(2), "cdf_gnur_2", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("weibull.ascii.gz");
    assertQuantile(new RayleighDistribution(MathUtil.SQRTHALF), "quant_scipy_2_1", 1e-15);
    assertQuantile(new RayleighDistribution(MathUtil.SQRTHALF), "quant_gnur_2_1", 1e-15);
    load("ray.ascii.gz");
    assertQuantile(new RayleighDistribution(1), "quant_scipy_1", 1e-14);
    assertQuantile(new RayleighDistribution(2), "quant_scipy_2", 1e-14);
    assertQuantile(new RayleighDistribution(1), "quant_gnur_1", 1e-14);
    assertQuantile(new RayleighDistribution(2), "quant_gnur_2", 1e-14);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("weibull.ascii.gz");
    Distribution dist = new ELKIBuilder<>(RayleighDistribution.class) //
        .with(RayleighDistribution.Par.SCALE_ID, MathUtil.SQRTHALF).build();
    assertPDF(dist, "pdf_scipy_2_1", 1e-15);

    load("ray.ascii.gz");
    dist = new ELKIBuilder<>(RayleighDistribution.class) //
        .with(RayleighDistribution.Par.SCALE_ID, 2).build();
    assertPDF(dist, "pdf_scipy_2", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new RayleighDistribution(0.1, 0.9), new Random(0L), 10000, 1e-2);
    assertRandom(new RayleighDistribution(1.41, 3.14), new Random(0L), 10000, 1e-2);
    assertRandom(new RayleighDistribution(3.14, 1.41), new Random(0L), 10000, 1e-2);
  }
}
