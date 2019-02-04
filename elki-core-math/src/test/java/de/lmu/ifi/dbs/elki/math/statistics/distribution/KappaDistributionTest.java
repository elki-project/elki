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
 * Unit test for the four-parameter Kappa distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KappaDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("kappa.ascii.gz");
    checkPDF(new KappaDistribution(.1, .2, .3, .4), "pdf_scipy_01_02_03_04", 1e-14);
    checkPDF(new KappaDistribution(.1, .2, .3, .4), "pdf_gnur_01_02_03_04", 1e-14);

    load("logistic.ascii.gz");
    checkPDF(new KappaDistribution(.1, 1., 0., -1.), "pdf_scipy_01", 1e-15);
    checkPDF(new KappaDistribution(.5, 1., 0., -1.), "pdf_scipy_05", 1e-15);
    checkPDF(new KappaDistribution(.1, 1., 0., -1.), "pdf_gnur_01", 1e-15);
    checkPDF(new KappaDistribution(.5, 1., 0., -1.), "pdf_gnur_05", 1e-15);

    load("gumbel.ascii.gz");
    checkPDF(new KappaDistribution(1., 1., 0., 0.), "pdf_scipy_1_1", 1e-15);
    checkPDF(new KappaDistribution(2., 1., 0., 0.), "pdf_scipy_2_1", 1e-15);
    checkPDF(new KappaDistribution(4., 1., 0., 0.), "pdf_scipy_4_1", 1e-15);
    checkPDF(new KappaDistribution(4., 10., 0., 0.), "pdf_scipy_4_10", 1e-15);
    checkPDF(new KappaDistribution(.1, 1., 0., 0.), "pdf_scipy_01_1", 1e-15);
    checkPDF(new KappaDistribution(.1, 4., 0., 0.), "pdf_scipy_01_4", 1e-15);
    checkPDF(new KappaDistribution(.1, 10., 0., 0.), "pdf_scipy_01_10", 1e-15);
    checkPDF(new KappaDistribution(.1, 20., 0., 0.), "pdf_scipy_01_20", 1e-15);

    checkPDF(new KappaDistribution(1., 1., 0., 0.), "pdf_gnur_1_1", 1e-15);
    checkPDF(new KappaDistribution(2., 1., 0., 0.), "pdf_gnur_2_1", 1e-15);
    checkPDF(new KappaDistribution(4., 1., 0., 0.), "pdf_gnur_4_1", 1e-15);
    checkPDF(new KappaDistribution(4., 10., 0., 0.), "pdf_gnur_4_10", 1e-15);
    checkPDF(new KappaDistribution(.1, 1., 0., 0.), "pdf_gnur_01_1", 1e-15);
    checkPDF(new KappaDistribution(.1, 4., 0., 0.), "pdf_gnur_01_4", 1e-15);
    checkPDF(new KappaDistribution(.1, 10., 0., 0.), "pdf_gnur_01_10", 1e-15);
    checkPDF(new KappaDistribution(.1, 20., 0., 0.), "pdf_gnur_01_20", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("kappa.ascii.gz");
    checkLogPDF(new KappaDistribution(.1, .2, .3, .4), "logpdf_scipy_01_02_03_04", 1e-14);

    load("logistic.ascii.gz");
    checkLogPDF(new KappaDistribution(.1, 1., 0., -1.), "logpdf_scipy_01", 1e-15);
    checkLogPDF(new KappaDistribution(.5, 1., 0., -1.), "logpdf_scipy_05", 1e-15);
    checkLogPDF(new KappaDistribution(.1, 1., 0., -1.), "logpdf_gnur_01", 1e-15);
    checkLogPDF(new KappaDistribution(.5, 1., 0., -1.), "logpdf_gnur_05", 1e-15);

    load("gumbel.ascii.gz");
    checkLogPDF(new KappaDistribution(1., 1., 0., 0.), "logpdf_scipy_1_1", 1e-15);
    checkLogPDF(new KappaDistribution(2., 1., 0., 0.), "logpdf_scipy_2_1", 1e-15);
    checkLogPDF(new KappaDistribution(4., 1., 0., 0.), "logpdf_scipy_4_1", 1e-15);
    checkLogPDF(new KappaDistribution(4., 10., 0., 0.), "logpdf_scipy_4_10", 1e-15);
    checkLogPDF(new KappaDistribution(.1, 1., 0., 0.), "logpdf_scipy_01_1", 1e-15);
    checkLogPDF(new KappaDistribution(.1, 4., 0., 0.), "logpdf_scipy_01_4", 1e-15);
    checkLogPDF(new KappaDistribution(.1, 10., 0., 0.), "logpdf_scipy_01_10", 1e-15);
    checkLogPDF(new KappaDistribution(.1, 20., 0., 0.), "logpdf_scipy_01_20", 1e-15);
  }

  @Test
  public void testCDF() {
    load("kappa.ascii.gz");
    checkCDF(new KappaDistribution(.1, .2, .3, .4), "cdf_gnur_01_02_03_04", 1e-14);
    checkCDF(new KappaDistribution(.1, .2, .3, .4), "cdf_scipy_01_02_03_04", 1e-14);

    load("logistic.ascii.gz");
    checkCDF(new KappaDistribution(.1, 1., 0., -1.), "cdf_scipy_01", 1e-15);
    checkCDF(new KappaDistribution(.5, 1., 0., -1.), "cdf_scipy_05", 1e-15);
    checkCDF(new KappaDistribution(.1, 1., 0., -1.), "cdf_gnur_01", 1e-15);
    checkCDF(new KappaDistribution(.5, 1., 0., -1.), "cdf_gnur_05", 1e-15);

    load("gumbel.ascii.gz");
    checkCDF(new KappaDistribution(1., 1., 0., 0.), "cdf_scipy_1_1", 1e-15);
    checkCDF(new KappaDistribution(2., 1., 0., 0.), "cdf_scipy_2_1", 1e-15);
    checkCDF(new KappaDistribution(4., 1., 0., 0.), "cdf_scipy_4_1", 1e-15);
    checkCDF(new KappaDistribution(4., 10., 0., 0.), "cdf_scipy_4_10", 1e-15);
    checkCDF(new KappaDistribution(.1, 1., 0., 0.), "cdf_scipy_01_1", 1e-15);
    checkCDF(new KappaDistribution(.1, 4., 0., 0.), "cdf_scipy_01_4", 1e-15);
    checkCDF(new KappaDistribution(.1, 10., 0., 0.), "cdf_scipy_01_10", 1e-15);
    checkCDF(new KappaDistribution(.1, 20., 0., 0.), "cdf_scipy_01_20", 1e-15);

    checkCDF(new KappaDistribution(1., 1., 0., 0.), "cdf_gnur_1_1", 1e-15);
    checkCDF(new KappaDistribution(2., 1., 0., 0.), "cdf_gnur_2_1", 1e-15);
    checkCDF(new KappaDistribution(4., 1., 0., 0.), "cdf_gnur_4_1", 1e-15);
    checkCDF(new KappaDistribution(4., 10., 0., 0.), "cdf_gnur_4_10", 1e-15);
    checkCDF(new KappaDistribution(.1, 1., 0., 0.), "cdf_gnur_01_1", 1e-15);
    checkCDF(new KappaDistribution(.1, 4., 0., 0.), "cdf_gnur_01_4", 1e-15);
    checkCDF(new KappaDistribution(.1, 10., 0., 0.), "cdf_gnur_01_10", 1e-15);
    checkCDF(new KappaDistribution(.1, 20., 0., 0.), "cdf_gnur_01_20", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("kappa.ascii.gz");
    checkQuantile(new KappaDistribution(.1, .2, .3, .4), "quant_gnur_01_02_03_04", 1e-15);
    checkQuantile(new KappaDistribution(.1, .2, .3, .4), "quant_scipy_01_02_03_04", 1e-14);

    load("logistic.ascii.gz");
    checkQuantile(new KappaDistribution(.1, 1., 0., -1.), "quant_scipy_01", 1e-13);
    checkQuantile(new KappaDistribution(.5, 1., 0., -1.), "quant_scipy_05", 1e-13);
    checkQuantile(new KappaDistribution(.1, 1., 0., -1.), "quant_gnur_01", 1e-13);
    checkQuantile(new KappaDistribution(.5, 1., 0., -1.), "quant_gnur_05", 1e-13);

    load("gumbel.ascii.gz");
    checkQuantile(new KappaDistribution(1., 1., 0., 0.), "quant_scipy_1_1", 1e-15);
    checkQuantile(new KappaDistribution(2., 1., 0., 0.), "quant_scipy_2_1", 1e-15);
    checkQuantile(new KappaDistribution(4., 1., 0., 0.), "quant_scipy_4_1", 1e-15);
    checkQuantile(new KappaDistribution(4., 10., 0., 0.), "quant_scipy_4_10", 1e-15);
    checkQuantile(new KappaDistribution(.1, 1., 0., 0.), "quant_scipy_01_1", 1e-15);
    checkQuantile(new KappaDistribution(.1, 4., 0., 0.), "quant_scipy_01_4", 1e-15);
    checkQuantile(new KappaDistribution(.1, 10., 0., 0.), "quant_scipy_01_10", 1e-15);
    checkQuantile(new KappaDistribution(.1, 20., 0., 0.), "quant_scipy_01_20", 1e-15);

    checkQuantile(new KappaDistribution(1., 1., 0., 0.), "quant_gnur_1_1", 1e-15);
    checkQuantile(new KappaDistribution(2., 1., 0., 0.), "quant_gnur_2_1", 1e-15);
    checkQuantile(new KappaDistribution(4., 1., 0., 0.), "quant_gnur_4_1", 1e-15);
    checkQuantile(new KappaDistribution(4., 10., 0., 0.), "quant_gnur_4_10", 1e-15);
    checkQuantile(new KappaDistribution(.1, 1., 0., 0.), "quant_gnur_01_1", 1e-15);
    checkQuantile(new KappaDistribution(.1, 4., 0., 0.), "quant_gnur_01_4", 1e-15);
    checkQuantile(new KappaDistribution(.1, 10., 0., 0.), "quant_gnur_01_10", 1e-15);
    checkQuantile(new KappaDistribution(.1, 20., 0., 0.), "quant_gnur_01_20", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("kappa.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(KappaDistribution.Parameterizer.LOCATION_ID, .1);
    params.addParameter(KappaDistribution.Parameterizer.SCALE_ID, .2);
    params.addParameter(KappaDistribution.Parameterizer.SHAPE1_ID, .3);
    params.addParameter(KappaDistribution.Parameterizer.SHAPE2_ID, .4);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(KappaDistribution.class, params);
    checkPDF(dist, "pdf_scipy_01_02_03_04", 1e-14);
  }

  @Test
  public void testRandom() {
    checkRandom(new KappaDistribution(1, 2, 0.1, 0.9, new Random(0L)), 10000, 1e-2);
    checkRandom(new KappaDistribution(3, 4, 1.41, 3.14, new Random(0L)), 10000, 1e-2);
    checkRandom(new KappaDistribution(5, 6, 3.14, 4.41, new Random(0L)), 10000, 1e-2);
  }
}
