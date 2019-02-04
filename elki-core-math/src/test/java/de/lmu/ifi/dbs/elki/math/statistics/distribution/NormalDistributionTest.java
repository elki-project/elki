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

import static org.junit.Assert.assertEquals;

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
public class NormalDistributionTest extends AbstractDistributionTest {
  @Test
  public void testERF() {
    assertEquals("Not NaN for NaN", Double.NaN, NormalDistribution.erf(Double.NaN), 0.);
  }

  @Test
  public void testPDF() {
    load("norm.ascii.gz");
    checkPDF(new NormalDistribution(0., 1.), "pdf_scipy_0_1", 1e-15);
    checkPDF(new NormalDistribution(1., 3.), "pdf_scipy_1_3", 1e-15);
    checkPDF(new NormalDistribution(.1, .1), "pdf_scipy_01_01", 1e-15);
    checkPDF(new NormalDistribution(0., 1.), "pdf_gnur_0_1", 1e-15);
    checkPDF(new NormalDistribution(1., 3.), "pdf_gnur_1_3", 1e-15);
    checkPDF(new NormalDistribution(.1, .1), "pdf_gnur_01_01", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("norm.ascii.gz");
    checkLogPDF(new NormalDistribution(0., 1.), "logpdf_scipy_0_1", 1e-15);
    checkLogPDF(new NormalDistribution(1., 3.), "logpdf_scipy_1_3", 1e-15);
    checkLogPDF(new NormalDistribution(.1, .1), "logpdf_scipy_01_01", 1e-15);
    checkLogPDF(new NormalDistribution(0., 1.), "logpdf_gnur_0_1", 1e-15);
    checkLogPDF(new NormalDistribution(1., 3.), "logpdf_gnur_1_3", 1e-15);
    checkLogPDF(new NormalDistribution(.1, .1), "logpdf_gnur_01_01", 1e-15);
  }

  @Test
  public void testCDF() {
    load("norm.ascii.gz");
    checkCDF(new NormalDistribution(0., 1.), "cdf_scipy_0_1", 1e-15);
    checkCDF(new NormalDistribution(1., 3.), "cdf_scipy_1_3", 1e-15);
    checkCDF(new NormalDistribution(.1, .1), "cdf_scipy_01_01", 1e-15);
    checkCDF(new NormalDistribution(0., 1.), "cdf_gnur_0_1", 1e-15);
    checkCDF(new NormalDistribution(1., 3.), "cdf_gnur_1_3", 1e-15);
    checkCDF(new NormalDistribution(.1, .1), "cdf_gnur_01_01", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("norm.ascii.gz");
    checkQuantile(new NormalDistribution(0., 1.), "quant_scipy_0_1", 1e-15);
    checkQuantile(new NormalDistribution(1., 3.), "quant_scipy_1_3", 1e-15);
    checkQuantile(new NormalDistribution(.1, .1), "quant_scipy_01_01", 1e-15);
    checkQuantile(new NormalDistribution(0., 1.), "quant_gnur_0_1", 1e-15);
    checkQuantile(new NormalDistribution(1., 3.), "quant_gnur_1_3", 1e-15);
    checkQuantile(new NormalDistribution(.1, .1), "quant_gnur_01_01", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("norm.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(NormalDistribution.Parameterizer.LOCATION_ID, 1.);
    params.addParameter(NormalDistribution.Parameterizer.SCALE_ID, 3.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(NormalDistribution.class, params);
    checkPDF(dist, "pdf_scipy_1_3", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new NormalDistribution(-1, 2, new Random(0L)), 10000, 1e-2);
    checkRandom(new NormalDistribution(1, 4, new Random(1L)), 10000, 1e-2);
  }
}
