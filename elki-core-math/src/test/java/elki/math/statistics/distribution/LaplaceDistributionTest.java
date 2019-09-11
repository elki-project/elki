/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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

import elki.utilities.ClassGenericsUtil;
import elki.utilities.exceptions.ClassInstantiationException;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the laplace (double exponential) distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LaplaceDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("lap.ascii.gz");
    checkPDF(new LaplaceDistribution(1, 3), "pdf_scipy_1_3", 1e-15);
    checkPDF(new LaplaceDistribution(4, .5), "pdf_scipy_4_05", 1e-15);

    checkPDF(new LaplaceDistribution(1, 3), "pdf_gnur_1_3", 1e-15);
    checkPDF(new LaplaceDistribution(4, .5), "pdf_gnur_4_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("lap.ascii.gz");
    checkLogPDF(new LaplaceDistribution(1, 3), "logpdf_scipy_1_3", 1e-15);
    checkLogPDF(new LaplaceDistribution(4, .5), "logpdf_scipy_4_05", 1e-15);
  }

  @Test
  public void testCDF() {
    load("lap.ascii.gz");
    checkCDF(new LaplaceDistribution(1, 3), "cdf_scipy_1_3", 1e-15);
    checkCDF(new LaplaceDistribution(4, .5), "cdf_scipy_4_05", 1e-15);

    checkCDF(new LaplaceDistribution(1, 3), "cdf_gnur_1_3", 1e-15);
    checkCDF(new LaplaceDistribution(4, .5), "cdf_gnur_4_05", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("lap.ascii.gz");
    checkQuantile(new LaplaceDistribution(1, 3), "quant_scipy_1_3", 1e-15);
    checkQuantile(new LaplaceDistribution(4, .5), "quant_scipy_4_05", 1e-15);

    checkQuantile(new LaplaceDistribution(1, 3), "quant_gnur_1_3", 1e-15);
    checkQuantile(new LaplaceDistribution(4, .5), "quant_gnur_4_05", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("lap.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(LaplaceDistribution.Par.RATE_ID, 1);
    params.addParameter(LaplaceDistribution.Par.LOCATION_ID, 3);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(LaplaceDistribution.class, params);
    checkPDF(dist, "pdf_scipy_1_3", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new LaplaceDistribution(0.1, 0.9), new Random(0L), 10000, 1e-2);
    checkRandom(new LaplaceDistribution(1.41, 3.14), new Random(0L), 10000, 1e-2);
    checkRandom(new LaplaceDistribution(3.14, 1.41), new Random(0L), 10000, 1e-2);
  }
}
