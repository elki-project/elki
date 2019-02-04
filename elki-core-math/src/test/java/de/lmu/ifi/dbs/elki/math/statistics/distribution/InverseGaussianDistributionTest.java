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
package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the inverse gaussian distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class InverseGaussianDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("invgauss.ascii.gz");
    checkPDF(new InverseGaussianDistribution(1., 1.), "pdf_gnur_1_1", 1e-15);
    checkPDF(new InverseGaussianDistribution(.5, 1.), "pdf_gnur_05_1", 1e-15);
    checkPDF(new InverseGaussianDistribution(1., .5), "pdf_gnur_1_05", 1e-15);

    checkPDF(new InverseGaussianDistribution(1., 1.), "pdf_scipy_1_1", 1e-15);
    checkPDF(new InverseGaussianDistribution(.5, 1.), "pdf_scipy_05_1", 1e-15);
    checkPDF(new InverseGaussianDistribution(1., .5), "pdf_scipy_1_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("invgauss.ascii.gz");
    checkLogPDF(new InverseGaussianDistribution(1., 1.), "logpdf_gnur_1_1", 1e-15);
    checkLogPDF(new InverseGaussianDistribution(.5, 1.), "logpdf_gnur_05_1", 1e-15);
    checkLogPDF(new InverseGaussianDistribution(1., .5), "logpdf_gnur_1_05", 1e-15);

    checkLogPDF(new InverseGaussianDistribution(1., 1.), "logpdf_scipy_1_1", 1e-15);
    checkLogPDF(new InverseGaussianDistribution(.5, 1.), "logpdf_scipy_05_1", 1e-15);
    checkLogPDF(new InverseGaussianDistribution(1., .5), "logpdf_scipy_1_05", 1e-14);
  }

  @Test
  public void testCDF() {
    load("invgauss.ascii.gz");
    checkCDF(new InverseGaussianDistribution(1., 1.), "cdf_gnur_1_1", 1e-14);
    checkCDF(new InverseGaussianDistribution(.5, 1.), "cdf_gnur_05_1", 1e-13);
    checkCDF(new InverseGaussianDistribution(1., .5), "cdf_gnur_1_05", 1e-14);

    checkCDF(new InverseGaussianDistribution(1., 1.), "cdf_scipy_1_1", 1e-13);
    checkCDF(new InverseGaussianDistribution(.5, 1.), "cdf_scipy_05_1", 1e-12);
    checkCDF(new InverseGaussianDistribution(1., .5), "cdf_scipy_1_05", 1e-14);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("invgauss.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(InverseGaussianDistribution.Parameterizer.LOCATION_ID, .5);
    params.addParameter(InverseGaussianDistribution.Parameterizer.SHAPE_ID, 1.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(InverseGaussianDistribution.class, params);
    checkPDF(dist, "pdf_scipy_05_1", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new InverseGaussianDistribution(0.1, 0.9, new Random(0L)), 10000, 1e-3);
    checkRandom(new InverseGaussianDistribution(1.41, 3.14, new Random(0L)), 10000, 1e-2);
    checkRandom(new InverseGaussianDistribution(3.14, 1.41, new Random(0L)), 10000, 1e-2);
  }
}
