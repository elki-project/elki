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
 * Unit test for the Generalized Extreme Value (GEV) distribution in ELKI.
 * 
 * The reference values were computed using SciPy nad R (lmomco).
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class GeneralizedExtremeValueDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    // Gumbel case:
    load("gumbel.ascii.gz");
    checkPDF(new GeneralizedExtremeValueDistribution(1., 1., 0.), "pdf_scipy_1_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(2., 1., 0.), "pdf_scipy_2_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(4., 1., 0.), "pdf_scipy_4_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(4., 10., 0.), "pdf_scipy_4_10", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 1., 0.), "pdf_scipy_01_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 4., 0.), "pdf_scipy_01_4", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 10., 0.), "pdf_scipy_01_10", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 20., 0.), "pdf_scipy_01_20", 1e-15);

    checkPDF(new GeneralizedExtremeValueDistribution(1., 1., 0.), "pdf_gnur_1_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(2., 1., 0.), "pdf_gnur_2_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(4., 1., 0.), "pdf_gnur_4_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(4., 10., 0.), "pdf_gnur_4_10", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 1., 0.), "pdf_gnur_01_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 4., 0.), "pdf_gnur_01_4", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 10., 0.), "pdf_gnur_01_10", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.1, 20., 0.), "pdf_gnur_01_20", 1e-15);
    // Regular:
    load("gev.ascii.gz");
    checkPDF(new GeneralizedExtremeValueDistribution(.2, 1, .8), "pdf_scipy_08_02_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, 1, 1.), "pdf_scipy_1_05_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "pdf_scipy_1_05_05", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "pdf_scipy_2_05_05", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "pdf_scipy_4_05_05", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, 1, -1.), "pdf_scipy_M1_05_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, -1.), "pdf_scipy_M1_05_05", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, -2.), "pdf_scipy_M2_05_05", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, -4.), "pdf_scipy_M4_05_05", 1e-15);

    // SciPy (like us) and GnuR differ at the end of the definition area.
    checkPDF(new GeneralizedExtremeValueDistribution(.2, 1, .8), "pdf_gnur_08_02_1", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, 1, 1.), "pdf_gnur_1_05_1", 1.);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "pdf_gnur_1_05_05", 2.);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "pdf_gnur_2_05_05", 1e-15);
    checkPDF(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "pdf_gnur_4_05_05", 1e-15);
    // GnuR also does not allow negative shape.
  }

  @Test
  public void testLogPDF() {
    // No log support in R

    // Gumbel case:
    load("gumbel.ascii.gz");
    checkLogPDF(new GeneralizedExtremeValueDistribution(1., 1., 0.), "logpdf_scipy_1_1", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(2., 1., 0.), "logpdf_scipy_2_1", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(4., 1., 0.), "logpdf_scipy_4_1", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(4., 10., 0.), "logpdf_scipy_4_10", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.1, 1., 0.), "logpdf_scipy_01_1", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.1, 4., 0.), "logpdf_scipy_01_4", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.1, 10., 0.), "logpdf_scipy_01_10", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.1, 20., 0.), "logpdf_scipy_01_20", 1e-15);

    // Regular:
    load("gev.ascii.gz");
    checkLogPDF(new GeneralizedExtremeValueDistribution(.2, 1, .8), "logpdf_scipy_08_02_1", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.5, 1, 1.), "logpdf_scipy_1_05_1", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "logpdf_scipy_1_05_05", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "logpdf_scipy_2_05_05", 1e-15);
    checkLogPDF(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "logpdf_scipy_4_05_05", 1e-15);
  }

  @Test
  public void testCDF() {
    // Gumbel case.
    load("gumbel.ascii.gz");
    checkCDF(new GeneralizedExtremeValueDistribution(1., 1., 0.), "cdf_scipy_1_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(2., 1., 0.), "cdf_scipy_2_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(4., 1., 0.), "cdf_scipy_4_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(4., 10., 0.), "cdf_scipy_4_10", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 1., 0.), "cdf_scipy_01_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 4., 0.), "cdf_scipy_01_4", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 10., 0.), "cdf_scipy_01_10", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 20., 0.), "cdf_scipy_01_20", 1e-15);

    checkCDF(new GeneralizedExtremeValueDistribution(1., 1., 0.), "cdf_gnur_1_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(2., 1., 0.), "cdf_gnur_2_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(4., 1., 0.), "cdf_gnur_4_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(4., 10., 0.), "cdf_gnur_4_10", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 1., 0.), "cdf_gnur_01_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 4., 0.), "cdf_gnur_01_4", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 10., 0.), "cdf_gnur_01_10", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.1, 20., 0.), "cdf_gnur_01_20", 1e-15);
    // Regular:
    load("gev.ascii.gz");
    checkCDF(new GeneralizedExtremeValueDistribution(.2, 1., .8), "cdf_scipy_08_02_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, 1., 1.), "cdf_scipy_1_05_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "cdf_scipy_1_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "cdf_scipy_2_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "cdf_scipy_4_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, 1., -1.), "cdf_scipy_M1_05_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, -1.), "cdf_scipy_M1_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, -2.), "cdf_scipy_M2_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, -4.), "cdf_scipy_M4_05_05", 1e-15);

    checkCDF(new GeneralizedExtremeValueDistribution(.2, 1., .8), "cdf_gnur_08_02_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, 1., 1.), "cdf_gnur_1_05_1", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "cdf_gnur_1_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "cdf_gnur_2_05_05", 1e-15);
    checkCDF(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "cdf_gnur_4_05_05", 1e-15);
  }

  @Test
  public void testQuantile() {
    // Gumbel case:
    load("gumbel.ascii.gz");
    checkQuantile(new GeneralizedExtremeValueDistribution(1., 1., 0.), "quant_scipy_1_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(2., 1., 0.), "quant_scipy_2_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(4., 1., 0.), "quant_scipy_4_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(4., 10., 0.), "quant_scipy_4_10", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.1, 1., 0.), "quant_scipy_01_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.1, 4., 0.), "quant_scipy_01_4", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.1, 10., 0.), "quant_scipy_01_10", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.1, 20., 0.), "quant_scipy_01_20", 1e-13);

    // Regular:
    load("gev.ascii.gz");
    checkQuantile(new GeneralizedExtremeValueDistribution(.2, 1., .8), "quant_scipy_08_02_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, 1., 1.), "quant_scipy_1_05_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "quant_scipy_1_05_05", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "quant_scipy_2_05_05", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "quant_scipy_4_05_05", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, 1., -1.), "quant_scipy_M1_05_1", 1e-15);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, -1.), "quant_scipy_M1_05_05", 1e-15);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, -2.), "quant_scipy_M2_05_05", 1e-15);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, -4.), "quant_scipy_M4_05_05", 1e-15);

    checkQuantile(new GeneralizedExtremeValueDistribution(.2, 1., .8), "quant_gnur_08_02_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, 1., 1.), "quant_gnur_1_05_1", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, 1.), "quant_gnur_1_05_05", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, 2.), "quant_gnur_2_05_05", 1e-13);
    checkQuantile(new GeneralizedExtremeValueDistribution(.5, .5, 4.), "quant_gnur_4_05_05", 1e-13);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("gev.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(GeneralizedExtremeValueDistribution.Parameterizer.SHAPE_ID, .8);
    params.addParameter(GeneralizedExtremeValueDistribution.Parameterizer.LOCATION_ID, .2);
    params.addParameter(GeneralizedExtremeValueDistribution.Parameterizer.SCALE_ID, 1.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(GeneralizedExtremeValueDistribution.class, params);
    checkPDF(dist, "pdf_scipy_08_02_1", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new GeneralizedExtremeValueDistribution(0.1, 0.9, 1, new Random(0L)), 10000, 1e-2);
    checkRandom(new GeneralizedExtremeValueDistribution(1.41, 3.14, 2, new Random(0L)), 10000, 1e-2);
    checkRandom(new GeneralizedExtremeValueDistribution(3.14, 1.41, 3, new Random(0L)), 10000, 1e-2);
  }
}
