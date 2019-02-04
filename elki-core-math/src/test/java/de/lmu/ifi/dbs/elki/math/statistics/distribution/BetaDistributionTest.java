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
 * Unit test for the Beta distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class BetaDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("beta.ascii.gz");
    checkPDF(new BetaDistribution(1., 1.), "pdf_scipy_1_1", 1e-15);
    checkPDF(new BetaDistribution(2., 1.), "pdf_scipy_2_1", 1e-14);
    checkPDF(new BetaDistribution(4., 1.), "pdf_scipy_4_1", 1e-14);
    checkPDF(new BetaDistribution(.1, 1.), "pdf_scipy_01_1", 1e-14);
    checkPDF(new BetaDistribution(.5, 1.), "pdf_scipy_05_1", 1e-14);
    checkPDF(new BetaDistribution(1., 2.), "pdf_scipy_1_2", 1e-15);
    checkPDF(new BetaDistribution(2., 2.), "pdf_scipy_2_2", 1e-14);
    checkPDF(new BetaDistribution(4., 2.), "pdf_scipy_4_2", 1e-14);
    checkPDF(new BetaDistribution(.1, 2.), "pdf_scipy_01_2", 1e-15);
    checkPDF(new BetaDistribution(.5, 2.), "pdf_scipy_05_2", 1e-14);
    checkPDF(new BetaDistribution(1., 4.), "pdf_scipy_1_4", 1e-14);
    checkPDF(new BetaDistribution(2., 4.), "pdf_scipy_2_4", 1e-14);
    checkPDF(new BetaDistribution(4., 4.), "pdf_scipy_4_4", 1e-14);
    checkPDF(new BetaDistribution(.1, 4.), "pdf_scipy_01_4", 1e-14);
    checkPDF(new BetaDistribution(.5, 4.), "pdf_scipy_05_4", 1e-15);
    checkPDF(new BetaDistribution(1., .1), "pdf_scipy_1_01", 1e-15);
    checkPDF(new BetaDistribution(2., .1), "pdf_scipy_2_01", 1e-15);
    checkPDF(new BetaDistribution(4., .1), "pdf_scipy_4_01", 1e-14);
    checkPDF(new BetaDistribution(.1, .1), "pdf_scipy_01_01", 1e-14);
    checkPDF(new BetaDistribution(.5, .1), "pdf_scipy_05_01", 1e-15);
    checkPDF(new BetaDistribution(1., .5), "pdf_scipy_1_05", 1e-14);
    checkPDF(new BetaDistribution(2., .5), "pdf_scipy_2_05", 1e-14);
    checkPDF(new BetaDistribution(4., .5), "pdf_scipy_4_05", 1e-15);
    checkPDF(new BetaDistribution(.1, .5), "pdf_scipy_01_05", 1e-15);
    checkPDF(new BetaDistribution(.5, .5), "pdf_scipy_05_05", 1e-14);

    checkPDF(new BetaDistribution(1., 1.), "pdf_gnur_1_1", 1e-15);
    checkPDF(new BetaDistribution(2., 1.), "pdf_gnur_2_1", 1e-14);
    checkPDF(new BetaDistribution(4., 1.), "pdf_gnur_4_1", 1e-14);
    checkPDF(new BetaDistribution(.1, 1.), "pdf_gnur_01_1", 1e-14);
    checkPDF(new BetaDistribution(.5, 1.), "pdf_gnur_05_1", 1e-14);
    checkPDF(new BetaDistribution(1., 2.), "pdf_gnur_1_2", 1e-15);
    checkPDF(new BetaDistribution(2., 2.), "pdf_gnur_2_2", 1e-14);
    checkPDF(new BetaDistribution(4., 2.), "pdf_gnur_4_2", 1e-13);
    checkPDF(new BetaDistribution(.1, 2.), "pdf_gnur_01_2", 1e-15);
    checkPDF(new BetaDistribution(.5, 2.), "pdf_gnur_05_2", 1e-14);
    checkPDF(new BetaDistribution(1., 4.), "pdf_gnur_1_4", 1e-14);
    checkPDF(new BetaDistribution(2., 4.), "pdf_gnur_2_4", 1e-14);
    checkPDF(new BetaDistribution(4., 4.), "pdf_gnur_4_4", 1e-14);
    checkPDF(new BetaDistribution(.1, 4.), "pdf_gnur_01_4", 1e-14);
    checkPDF(new BetaDistribution(.5, 4.), "pdf_gnur_05_4", 1e-15);
    checkPDF(new BetaDistribution(1., .1), "pdf_gnur_1_01", 1e-15);
    checkPDF(new BetaDistribution(2., .1), "pdf_gnur_2_01", 1e-15);
    checkPDF(new BetaDistribution(4., .1), "pdf_gnur_4_01", 1e-14);
    checkPDF(new BetaDistribution(.1, .1), "pdf_gnur_01_01", 1e-14);
    checkPDF(new BetaDistribution(.5, .1), "pdf_gnur_05_01", 1e-15);
    checkPDF(new BetaDistribution(1., .5), "pdf_gnur_1_05", 1e-14);
    checkPDF(new BetaDistribution(2., .5), "pdf_gnur_2_05", 1e-14);
    checkPDF(new BetaDistribution(4., .5), "pdf_gnur_4_05", 1e-15);
    checkPDF(new BetaDistribution(.1, .5), "pdf_gnur_01_05", 1e-15);
    checkPDF(new BetaDistribution(.5, .5), "pdf_gnur_05_05", 1e-14);
  }

  @Test
  public void testLogPDF() {
    load("beta.ascii.gz");
    checkLogPDF(new BetaDistribution(1., 1.), "logpdf_scipy_1_1", 1e-15);
    checkLogPDF(new BetaDistribution(2., 1.), "logpdf_scipy_2_1", 1e-14);
    checkLogPDF(new BetaDistribution(4., 1.), "logpdf_scipy_4_1", 1e-14);
    checkLogPDF(new BetaDistribution(.1, 1.), "logpdf_scipy_01_1", 1e-14);
    checkLogPDF(new BetaDistribution(.5, 1.), "logpdf_scipy_05_1", 1e-14);
    checkLogPDF(new BetaDistribution(1., 2.), "logpdf_scipy_1_2", 1e-15);
    checkLogPDF(new BetaDistribution(2., 2.), "logpdf_scipy_2_2", 1e-15);
    checkLogPDF(new BetaDistribution(4., 2.), "logpdf_scipy_4_2", 1e-14);
    checkLogPDF(new BetaDistribution(.1, 2.), "logpdf_scipy_01_2", 1e-15);
    checkLogPDF(new BetaDistribution(.5, 2.), "logpdf_scipy_05_2", 1e-14);
    checkLogPDF(new BetaDistribution(1., 4.), "logpdf_scipy_1_4", 1e-14);
    checkLogPDF(new BetaDistribution(2., 4.), "logpdf_scipy_2_4", 1e-15);
    checkLogPDF(new BetaDistribution(4., 4.), "logpdf_scipy_4_4", 1e-14);
    checkLogPDF(new BetaDistribution(.1, 4.), "logpdf_scipy_01_4", 1e-14);
    checkLogPDF(new BetaDistribution(.5, 4.), "logpdf_scipy_05_4", 1e-15);
    checkLogPDF(new BetaDistribution(1., .1), "logpdf_scipy_1_01", 1e-15);
    checkLogPDF(new BetaDistribution(2., .1), "logpdf_scipy_2_01", 1e-15);
    checkLogPDF(new BetaDistribution(4., .1), "logpdf_scipy_4_01", 1e-14);
    checkLogPDF(new BetaDistribution(.1, .1), "logpdf_scipy_01_01", 1e-14);
    checkLogPDF(new BetaDistribution(.5, .1), "logpdf_scipy_05_01", 1e-15);
    checkLogPDF(new BetaDistribution(1., .5), "logpdf_scipy_1_05", 1e-14);
    checkLogPDF(new BetaDistribution(2., .5), "logpdf_scipy_2_05", 1e-14);
    checkLogPDF(new BetaDistribution(4., .5), "logpdf_scipy_4_05", 1e-15);
    checkLogPDF(new BetaDistribution(.1, .5), "logpdf_scipy_01_05", 1e-15);
    checkLogPDF(new BetaDistribution(.5, .5), "logpdf_scipy_05_05", 1e-14);

    checkLogPDF(new BetaDistribution(1., 1.), "logpdf_gnur_1_1", 1e-15);
    checkLogPDF(new BetaDistribution(2., 1.), "logpdf_gnur_2_1", 1e-14);
    checkLogPDF(new BetaDistribution(4., 1.), "logpdf_gnur_4_1", 1e-14);
    checkLogPDF(new BetaDistribution(.1, 1.), "logpdf_gnur_01_1", 1e-14);
    checkLogPDF(new BetaDistribution(.5, 1.), "logpdf_gnur_05_1", 1e-14);
    checkLogPDF(new BetaDistribution(1., 2.), "logpdf_gnur_1_2", 1e-15);
    checkLogPDF(new BetaDistribution(2., 2.), "logpdf_gnur_2_2", 1e-15);
    checkLogPDF(new BetaDistribution(4., 2.), "logpdf_gnur_4_2", 1e-13);
    checkLogPDF(new BetaDistribution(.1, 2.), "logpdf_gnur_01_2", 1e-15);
    checkLogPDF(new BetaDistribution(.5, 2.), "logpdf_gnur_05_2", 1e-14);
    checkLogPDF(new BetaDistribution(1., 4.), "logpdf_gnur_1_4", 1e-14);
    checkLogPDF(new BetaDistribution(2., 4.), "logpdf_gnur_2_4", 1e-15);
    checkLogPDF(new BetaDistribution(4., 4.), "logpdf_gnur_4_4", 1e-14);
    checkLogPDF(new BetaDistribution(.1, 4.), "logpdf_gnur_01_4", 1e-14);
    checkLogPDF(new BetaDistribution(.5, 4.), "logpdf_gnur_05_4", 1e-15);
    checkLogPDF(new BetaDistribution(1., .1), "logpdf_gnur_1_01", 1e-15);
    checkLogPDF(new BetaDistribution(2., .1), "logpdf_gnur_2_01", 1e-15);
    checkLogPDF(new BetaDistribution(4., .1), "logpdf_gnur_4_01", 1e-14);
    checkLogPDF(new BetaDistribution(.1, .1), "logpdf_gnur_01_01", 1e-14);
    checkLogPDF(new BetaDistribution(.5, .1), "logpdf_gnur_05_01", 1e-15);
    checkLogPDF(new BetaDistribution(1., .5), "logpdf_gnur_1_05", 1e-14);
    checkLogPDF(new BetaDistribution(2., .5), "logpdf_gnur_2_05", 1e-14);
    checkLogPDF(new BetaDistribution(4., .5), "logpdf_gnur_4_05", 1e-15);
    checkLogPDF(new BetaDistribution(.1, .5), "logpdf_gnur_01_05", 1e-15);
    checkLogPDF(new BetaDistribution(.5, .5), "logpdf_gnur_05_05", 1e-14);
  }

  @Test
  public void testCDF() {
    load("beta.ascii.gz");
    checkCDF(new BetaDistribution(1., 1.), "cdf_scipy_1_1", 1e-14);
    checkCDF(new BetaDistribution(2., 1.), "cdf_scipy_2_1", 1e-15);
    checkCDF(new BetaDistribution(4., 1.), "cdf_scipy_4_1", 1e-14);
    checkCDF(new BetaDistribution(.1, 1.), "cdf_scipy_01_1", 1e-15);
    checkCDF(new BetaDistribution(.5, 1.), "cdf_scipy_05_1", 1e-14);
    checkCDF(new BetaDistribution(1., 2.), "cdf_scipy_1_2", 1e-14);
    checkCDF(new BetaDistribution(2., 2.), "cdf_scipy_2_2", 1e-14);
    checkCDF(new BetaDistribution(4., 2.), "cdf_scipy_4_2", 1e-14);
    checkCDF(new BetaDistribution(.1, 2.), "cdf_scipy_01_2", 1e-15);
    checkCDF(new BetaDistribution(.5, 2.), "cdf_scipy_05_2", 1e-15);
    checkCDF(new BetaDistribution(1., 4.), "cdf_scipy_1_4", 1e-14);
    checkCDF(new BetaDistribution(2., 4.), "cdf_scipy_2_4", 1e-14);
    checkCDF(new BetaDistribution(4., 4.), "cdf_scipy_4_4", 1e-14);
    checkCDF(new BetaDistribution(.1, 4.), "cdf_scipy_01_4", 1e-14);
    checkCDF(new BetaDistribution(.5, 4.), "cdf_scipy_05_4", 1e-15);
    checkCDF(new BetaDistribution(1., .1), "cdf_scipy_1_01", 1e-14);
    checkCDF(new BetaDistribution(2., .1), "cdf_scipy_2_01", 1e-14);
    checkCDF(new BetaDistribution(4., .1), "cdf_scipy_4_01", 1e-14);
    checkCDF(new BetaDistribution(.1, .1), "cdf_scipy_01_01", 1e-14);
    checkCDF(new BetaDistribution(.5, .1), "cdf_scipy_05_01", 1e-14);
    checkCDF(new BetaDistribution(1., .5), "cdf_scipy_1_05", 1e-14);
    checkCDF(new BetaDistribution(2., .5), "cdf_scipy_2_05", 1e-14);
    checkCDF(new BetaDistribution(4., .5), "cdf_scipy_4_05", 1e-14);
    checkCDF(new BetaDistribution(.1, .5), "cdf_scipy_01_05", 1e-15);
    checkCDF(new BetaDistribution(.5, .5), "cdf_scipy_05_05", 1e-14);

    checkCDF(new BetaDistribution(1., 1.), "cdf_gnur_1_1", 1e-14);
    checkCDF(new BetaDistribution(2., 1.), "cdf_gnur_2_1", 1e-14);
    checkCDF(new BetaDistribution(4., 1.), "cdf_gnur_4_1", 1e-13);
    checkCDF(new BetaDistribution(.1, 1.), "cdf_gnur_01_1", 1e-15);
    checkCDF(new BetaDistribution(.5, 1.), "cdf_gnur_05_1", 1e-14);
    checkCDF(new BetaDistribution(1., 2.), "cdf_gnur_1_2", 1e-14);
    checkCDF(new BetaDistribution(2., 2.), "cdf_gnur_2_2", 1e-14);
    checkCDF(new BetaDistribution(4., 2.), "cdf_gnur_4_2", 1e-14);
    checkCDF(new BetaDistribution(.1, 2.), "cdf_gnur_01_2", 1e-15);
    checkCDF(new BetaDistribution(.5, 2.), "cdf_gnur_05_2", 1e-14);
    checkCDF(new BetaDistribution(1., 4.), "cdf_gnur_1_4", 1e-14);
    checkCDF(new BetaDistribution(2., 4.), "cdf_gnur_2_4", 1e-15);
    checkCDF(new BetaDistribution(4., 4.), "cdf_gnur_4_4", 1e-14);
    checkCDF(new BetaDistribution(.1, 4.), "cdf_gnur_01_4", 1e-15);
    checkCDF(new BetaDistribution(.5, 4.), "cdf_gnur_05_4", 1e-15);
    checkCDF(new BetaDistribution(1., .1), "cdf_gnur_1_01", 1e-14);
    checkCDF(new BetaDistribution(2., .1), "cdf_gnur_2_01", 1e-14);
    checkCDF(new BetaDistribution(4., .1), "cdf_gnur_4_01", 1e-14);
    checkCDF(new BetaDistribution(.1, .1), "cdf_gnur_01_01", 1e-14);
    checkCDF(new BetaDistribution(.5, .1), "cdf_gnur_05_01", 1e-14);
    checkCDF(new BetaDistribution(1., .5), "cdf_gnur_1_05", 1e-14);
    checkCDF(new BetaDistribution(2., .5), "cdf_gnur_2_05", 1e-14);
    checkCDF(new BetaDistribution(4., .5), "cdf_gnur_4_05", 1e-13);
    checkCDF(new BetaDistribution(.1, .5), "cdf_gnur_01_05", 1e-15);
    checkCDF(new BetaDistribution(.5, .5), "cdf_gnur_05_05", 1e-14);
  }

  @Test
  public void testQuantile() {
    load("beta.ascii.gz");
    checkQuantile(new BetaDistribution(1., 1.), "quant_scipy_1_1", 1e-15);
    checkQuantile(new BetaDistribution(2., 1.), "quant_scipy_2_1", 1e-15);
    checkQuantile(new BetaDistribution(4., 1.), "quant_scipy_4_1", 1e-15);
    checkQuantile(new BetaDistribution(.1, 1.), "quant_scipy_01_1", 1e-14);
    checkQuantile(new BetaDistribution(.5, 1.), "quant_scipy_05_1", 1e-14);
    checkQuantile(new BetaDistribution(1., 2.), "quant_scipy_1_2", 1e-14);
    checkQuantile(new BetaDistribution(2., 2.), "quant_scipy_2_2", 1e-15);
    checkQuantile(new BetaDistribution(4., 2.), "quant_scipy_4_2", 1e-15);
    checkQuantile(new BetaDistribution(.1, 2.), "quant_scipy_01_2", 1e-13);
    checkQuantile(new BetaDistribution(.5, 2.), "quant_scipy_05_2", 1e-14);
    checkQuantile(new BetaDistribution(1., 4.), "quant_scipy_1_4", 1e-14);
    checkQuantile(new BetaDistribution(2., 4.), "quant_scipy_2_4", 1e-15);
    checkQuantile(new BetaDistribution(4., 4.), "quant_scipy_4_4", 1e-15);
    checkQuantile(new BetaDistribution(.1, 4.), "quant_scipy_01_4", 1e-13);
    checkQuantile(new BetaDistribution(.5, 4.), "quant_scipy_05_4", 1e-14);
    checkQuantile(new BetaDistribution(1., .1), "quant_scipy_1_01", 1e-14);
    checkQuantile(new BetaDistribution(2., .1), "quant_scipy_2_01", 1e-15);
    checkQuantile(new BetaDistribution(4., .1), "quant_scipy_4_01", 1e-15);
    checkQuantile(new BetaDistribution(.1, .1), "quant_scipy_01_01", 1e-13);
    checkQuantile(new BetaDistribution(.5, .1), "quant_scipy_05_01", 1e-14);
    checkQuantile(new BetaDistribution(1., .5), "quant_scipy_1_05", 1e-14);
    checkQuantile(new BetaDistribution(2., .5), "quant_scipy_2_05", 1e-15);
    checkQuantile(new BetaDistribution(4., .5), "quant_scipy_4_05", 1e-15);
    checkQuantile(new BetaDistribution(.1, .5), "quant_scipy_01_05", 1e-14);
    checkQuantile(new BetaDistribution(.5, .5), "quant_scipy_05_05", 1e-14);
    checkQuantile(new BetaDistribution(5000, 10000), "quant_scipy_5000_10000", 1e-13);

    checkQuantile(new BetaDistribution(1., 1.), "quant_gnur_1_1", 1e-15);
    checkQuantile(new BetaDistribution(2., 1.), "quant_gnur_2_1", 1e-15);
    checkQuantile(new BetaDistribution(4., 1.), "quant_gnur_4_1", 1e-15);
    checkQuantile(new BetaDistribution(.1, 1.), "quant_gnur_01_1", 1e-13);
    checkQuantile(new BetaDistribution(.5, 1.), "quant_gnur_05_1", 1e-14);
    checkQuantile(new BetaDistribution(1., 2.), "quant_gnur_1_2", 1e-14);
    checkQuantile(new BetaDistribution(2., 2.), "quant_gnur_2_2", 1e-15);
    checkQuantile(new BetaDistribution(4., 2.), "quant_gnur_4_2", 1e-15);
    checkQuantile(new BetaDistribution(.1, 2.), "quant_gnur_01_2", 1e-14);
    checkQuantile(new BetaDistribution(.5, 2.), "quant_gnur_05_2", 1e-14);
    checkQuantile(new BetaDistribution(1., 4.), "quant_gnur_1_4", 1e-14);
    checkQuantile(new BetaDistribution(2., 4.), "quant_gnur_2_4", 1e-15);
    checkQuantile(new BetaDistribution(4., 4.), "quant_gnur_4_4", 1e-15);
    checkQuantile(new BetaDistribution(.1, 4.), "quant_gnur_01_4", 1e-13);
    checkQuantile(new BetaDistribution(.5, 4.), "quant_gnur_05_4", 1e-14);
    checkQuantile(new BetaDistribution(1., .1), "quant_gnur_1_01", 1e-14);
    checkQuantile(new BetaDistribution(2., .1), "quant_gnur_2_01", 1e-15);
    checkQuantile(new BetaDistribution(4., .1), "quant_gnur_4_01", 1e-15);
    checkQuantile(new BetaDistribution(.1, .1), "quant_gnur_01_01", 1e-14);
    checkQuantile(new BetaDistribution(.5, .1), "quant_gnur_05_01", 1e-14);
    checkQuantile(new BetaDistribution(1., .5), "quant_gnur_1_05", 1e-14);
    checkQuantile(new BetaDistribution(2., .5), "quant_gnur_2_05", 1e-15);
    checkQuantile(new BetaDistribution(4., .5), "quant_gnur_4_05", 1e-15);
    checkQuantile(new BetaDistribution(.1, .5), "quant_gnur_01_05", 1e-14);
    checkQuantile(new BetaDistribution(.5, .5), "quant_gnur_05_05", 1e-14);
    checkQuantile(new BetaDistribution(5000, 10000), "quant_gnur_5000_10000", 1e-13);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("beta.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(BetaDistribution.Parameterizer.ALPHA_ID, 2.);
    params.addParameter(BetaDistribution.Parameterizer.BETA_ID, 1.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(BetaDistribution.class, params);
    checkPDF(dist, "pdf_scipy_2_1", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new BetaDistribution(0.1, 0.9, new Random(0L)), 10000, 1e-2);
    checkRandom(new BetaDistribution(1.41, 3.14, new Random(0L)), 10000, 1e-2);
    checkRandom(new BetaDistribution(3.14, 1.41, new Random(0L)), 10000, 1e-2);
  }
}
