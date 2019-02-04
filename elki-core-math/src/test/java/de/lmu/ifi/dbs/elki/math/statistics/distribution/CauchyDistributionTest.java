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
 * Unit test for the Cauchy distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CauchyDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("cauchy.ascii.gz");
    checkPDF(new CauchyDistribution(.5, 1.), "pdf_gnur_05_1", 1e-15);
    checkPDF(new CauchyDistribution(1., .5), "pdf_gnur_1_05", 1e-15);

    checkPDF(new CauchyDistribution(.5, 1.), "pdf_scipy_05_1", 1e-15);
    checkPDF(new CauchyDistribution(1., .5), "pdf_scipy_1_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("cauchy.ascii.gz");
    checkLogPDF(new CauchyDistribution(.5, 1.), "logpdf_gnur_05_1", 1e-15);
    checkLogPDF(new CauchyDistribution(1., .5), "logpdf_gnur_1_05", 1e-15);

    checkLogPDF(new CauchyDistribution(.5, 1.), "logpdf_scipy_05_1", 1e-15);
    checkLogPDF(new CauchyDistribution(1., .5), "logpdf_scipy_1_05", 1e-14);
  }

  @Test
  public void testCDF() {
    load("cauchy.ascii.gz");
    checkCDF(new CauchyDistribution(1., .5), "cdf_gnur_1_05", 1e-15);
    checkCDF(new CauchyDistribution(.5, 1.), "cdf_gnur_05_1", 1e-15);

    checkCDF(new CauchyDistribution(1., .5), "cdf_scipy_1_05", 1e-15);
    checkCDF(new CauchyDistribution(.5, 1.), "cdf_scipy_05_1", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("cauchy.ascii.gz");
    checkQuantile(new CauchyDistribution(1., .5), "quant_gnur_1_05", 1e-15);
    checkQuantile(new CauchyDistribution(.5, 1.), "quant_gnur_05_1", 1e-15);

    checkQuantile(new CauchyDistribution(1., .5), "quant_scipy_1_05", 1e-12);
    checkQuantile(new CauchyDistribution(.5, 1.), "quant_scipy_05_1", 1e-12);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("cauchy.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(CauchyDistribution.Parameterizer.LOCATION_ID, .5);
    params.addParameter(CauchyDistribution.Parameterizer.SHAPE_ID, 1.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(CauchyDistribution.class, params);
    checkPDF(dist, "pdf_gnur_05_1", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new CauchyDistribution(0.1, 0.9, new Random(0L)), 10000, 1e-2);
    checkRandom(new CauchyDistribution(1.41, 3.14, new Random(0L)), 10000, 1e-2);
    checkRandom(new CauchyDistribution(3.14, 1.41, new Random(0L)), 10000, 1e-2);
  }
}
