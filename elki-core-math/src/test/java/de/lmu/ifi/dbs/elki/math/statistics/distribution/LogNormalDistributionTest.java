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
 * Unit test for the Normal distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LogNormalDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("lognorm.ascii.gz");
    checkPDF(new LogNormalDistribution(0., 1., 0), "pdf_gnur_0_1", 1e-15);
    checkPDF(new LogNormalDistribution(1., 3., 0), "pdf_gnur_1_3", 1e-15);
    checkPDF(new LogNormalDistribution(.1, .1, 0), "pdf_gnur_01_01", 1e-15);
    checkPDF(new LogNormalDistribution(0., 1., 0), "pdf_scipy_0_1", 1e-14);
    checkPDF(new LogNormalDistribution(1., 3., 0), "pdf_scipy_1_3", 1e-14);
    checkPDF(new LogNormalDistribution(.1, .1, 0), "pdf_scipy_01_01", 1e-14);
  }

  @Test
  public void testLogPDF() {
    load("lognorm.ascii.gz");
    checkLogPDF(new LogNormalDistribution(0., 1., 0), "logpdf_gnur_0_1", 1e-15);
    checkLogPDF(new LogNormalDistribution(1., 3., 0), "logpdf_gnur_1_3", 1e-15);
    checkLogPDF(new LogNormalDistribution(.1, .1, 0), "logpdf_gnur_01_01", 1e-15);
    checkLogPDF(new LogNormalDistribution(0., 1., 0), "logpdf_scipy_0_1", 1e-14);
    checkLogPDF(new LogNormalDistribution(1., 3., 0), "logpdf_scipy_1_3", 1e-14);
    checkLogPDF(new LogNormalDistribution(.1, .1, 0), "logpdf_scipy_01_01", 1e-14);
  }

  @Test
  public void testCDF() {
    load("lognorm.ascii.gz");
    checkCDF(new LogNormalDistribution(0., 1., 0), "cdf_gnur_0_1", 1e-15);
    checkCDF(new LogNormalDistribution(1., 3., 0), "cdf_gnur_1_3", 1e-15);
    checkCDF(new LogNormalDistribution(.1, .1, 0), "cdf_gnur_01_01", 1e-15);
    checkCDF(new LogNormalDistribution(0., 1., 0), "cdf_scipy_0_1", 1e-12);
    checkCDF(new LogNormalDistribution(1., 3., 0), "cdf_scipy_1_3", 1e-12);
    checkCDF(new LogNormalDistribution(.1, .1, 0), "cdf_scipy_01_01", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("lognorm.ascii.gz");
    checkQuantile(new LogNormalDistribution(0., 1., 0), "quant_gnur_0_1", 1e-15);
    checkQuantile(new LogNormalDistribution(1., 3., 0), "quant_gnur_1_3", 1e-14);
    checkQuantile(new LogNormalDistribution(.1, .1, 0), "quant_gnur_01_01", 1e-15);
    checkQuantile(new LogNormalDistribution(0., 1., 0), "quant_scipy_0_1", 1e-15);
    checkQuantile(new LogNormalDistribution(1., 3., 0), "quant_scipy_1_3", 1e-14);
    checkQuantile(new LogNormalDistribution(.1, .1, 0), "quant_scipy_01_01", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("lognorm.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(LogNormalDistribution.Parameterizer.LOGMEAN_ID, 1.);
    params.addParameter(LogNormalDistribution.Parameterizer.LOGSTDDEV_ID, 3.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(LogNormalDistribution.class, params);
    checkPDF(dist, "pdf_scipy_1_3", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new LogNormalDistribution(0.1, 0.9, 1, new Random(0L)), 10000, 1e-2);
    checkRandom(new LogNormalDistribution(1.41, 3.14, 2, new Random(0L)), 10000, 1e-2);
    checkRandom(new LogNormalDistribution(3.14, 1.41, 3, new Random(0L)), 10000, 1e-2);
  }
}
