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
 * The reference values were computed using SciPy and GNU R.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ExponentiallyModifiedGaussianDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("emg.ascii.gz");
    assertPDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), "pdf_scipy_1_3_05", 1e-15);
    assertPDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .1), "pdf_scipy_1_3_01", 1e-15);
    assertPDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), "pdf_gnur_1_3_05", 1e-15);
    assertPDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .1), "pdf_gnur_1_3_01", 1e-15);
  }

  @Test
  public void testCDF() {
    load("emg.ascii.gz");
    assertCDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), "cdf_scipy_1_3_05", 1e-14);
    assertCDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .1), "cdf_scipy_1_3_01", 1e-14);
    assertCDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), "cdf_gnur_1_3_05", 1e-14);
    assertCDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .1), "cdf_gnur_1_3_01", 1e-14);
  }

  // TODO: once quantile() is implemented, add a test.

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("emg.ascii.gz");
    Distribution dist = new ELKIBuilder<>(ExponentiallyModifiedGaussianDistribution.class) //
        .with(ExponentiallyModifiedGaussianDistribution.Par.LOCATION_ID, 1.) //
        .with(ExponentiallyModifiedGaussianDistribution.Par.SCALE_ID, 3) //
        .with(ExponentiallyModifiedGaussianDistribution.Par.RATE_ID, .5).build();
    assertPDF(dist, "pdf_gnur_1_3_05", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new ExponentiallyModifiedGaussianDistribution(0.1, 0.9, 1.), new Random(0L), 10000, 1e-2);
    assertRandom(new ExponentiallyModifiedGaussianDistribution(1.41, 3.14, 2.), new Random(0L), 10000, 1e-2);
    assertRandom(new ExponentiallyModifiedGaussianDistribution(3.14, 1.41, 3.), new Random(0L), 10000, 1e-2);
  }
}
