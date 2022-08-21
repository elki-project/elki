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
 * Unit test for the inverse gaussian distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class InverseGaussianDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("invgauss.ascii.gz");
    assertPDF(new InverseGaussianDistribution(1., 1.), "pdf_gnur_1_1", 1e-15);
    assertPDF(new InverseGaussianDistribution(.5, 1.), "pdf_gnur_05_1", 1e-15);
    assertPDF(new InverseGaussianDistribution(1., .5), "pdf_gnur_1_05", 1e-15);

    assertPDF(new InverseGaussianDistribution(1., 1.), "pdf_scipy_1_1", 1e-15);
    assertPDF(new InverseGaussianDistribution(.5, 1.), "pdf_scipy_05_1", 1e-15);
    assertPDF(new InverseGaussianDistribution(1., .5), "pdf_scipy_1_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("invgauss.ascii.gz");
    assertLogPDF(new InverseGaussianDistribution(1., 1.), "logpdf_gnur_1_1", 1e-15);
    assertLogPDF(new InverseGaussianDistribution(.5, 1.), "logpdf_gnur_05_1", 1e-15);
    assertLogPDF(new InverseGaussianDistribution(1., .5), "logpdf_gnur_1_05", 1e-15);

    assertLogPDF(new InverseGaussianDistribution(1., 1.), "logpdf_scipy_1_1", 1e-15);
    assertLogPDF(new InverseGaussianDistribution(.5, 1.), "logpdf_scipy_05_1", 1e-15);
    assertLogPDF(new InverseGaussianDistribution(1., .5), "logpdf_scipy_1_05", 1e-14);
  }

  @Test
  public void testCDF() {
    load("invgauss.ascii.gz");
    assertCDF(new InverseGaussianDistribution(1., 1.), "cdf_gnur_1_1", 1e-14);
    assertCDF(new InverseGaussianDistribution(.5, 1.), "cdf_gnur_05_1", 1e-13);
    assertCDF(new InverseGaussianDistribution(1., .5), "cdf_gnur_1_05", 1e-14);

    assertCDF(new InverseGaussianDistribution(1., 1.), "cdf_scipy_1_1", 1e-13);
    assertCDF(new InverseGaussianDistribution(.5, 1.), "cdf_scipy_05_1", 1e-12);
    assertCDF(new InverseGaussianDistribution(1., .5), "cdf_scipy_1_05", 1e-14);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("invgauss.ascii.gz");
    Distribution dist = new ELKIBuilder<>(InverseGaussianDistribution.class) //
        .with(InverseGaussianDistribution.Par.LOCATION_ID, .5) //
        .with(InverseGaussianDistribution.Par.SHAPE_ID, 1.).build();
    assertPDF(dist, "pdf_scipy_05_1", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new InverseGaussianDistribution(0.1, 0.9), new Random(0L), 10000, 1e-3);
    assertRandom(new InverseGaussianDistribution(1.41, 3.14), new Random(0L), 10000, 1e-2);
    assertRandom(new InverseGaussianDistribution(3.14, 1.41), new Random(0L), 10000, 1e-2);
  }
}
