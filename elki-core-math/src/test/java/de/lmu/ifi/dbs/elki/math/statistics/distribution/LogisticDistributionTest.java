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
 * Unit test for the Logistic distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LogisticDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("logistic.ascii.gz");
    checkPDF(new LogisticDistribution(.1, 1.), "pdf_scipy_01", 1e-15);
    checkPDF(new LogisticDistribution(.5, 1.), "pdf_scipy_05", 1e-15);
    checkPDF(new LogisticDistribution(.1, 1.), "pdf_gnur_01", 1e-15);
    checkPDF(new LogisticDistribution(.5, 1.), "pdf_gnur_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("logistic.ascii.gz");
    checkLogPDF(new LogisticDistribution(.1, 1.), "logpdf_scipy_01", 1e-15);
    checkLogPDF(new LogisticDistribution(.5, 1.), "logpdf_scipy_05", 1e-15);
    checkLogPDF(new LogisticDistribution(.1, 1.), "logpdf_gnur_01", 1e-15);
    checkLogPDF(new LogisticDistribution(.5, 1.), "logpdf_gnur_05", 1e-15);
  }

  @Test
  public void testCDF() {
    load("logistic.ascii.gz");
    checkCDF(new LogisticDistribution(.1, 1.), "cdf_scipy_01", 1e-15);
    checkCDF(new LogisticDistribution(.5, 1.), "cdf_scipy_05", 1e-15);
    checkCDF(new LogisticDistribution(.1, 1.), "cdf_gnur_01", 1e-15);
    checkCDF(new LogisticDistribution(.5, 1.), "cdf_gnur_05", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("logistic.ascii.gz");
    checkQuantile(new LogisticDistribution(.1, 1.), "quant_scipy_01", 1e-15);
    checkQuantile(new LogisticDistribution(.5, 1.), "quant_scipy_05", 1e-15);
    checkQuantile(new LogisticDistribution(.1, 1.), "quant_gnur_01", 1e-15);
    checkQuantile(new LogisticDistribution(.5, 1.), "quant_gnur_05", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("logistic.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(LogisticDistribution.Parameterizer.LOCATION_ID, .1);
    params.addParameter(LogisticDistribution.Parameterizer.SCALE_ID, 1.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(LogisticDistribution.class, params);
    checkPDF(dist, "pdf_scipy_01", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new LogisticDistribution(0.1, 0.9, new Random(0L)), 10000, 1e-2);
    checkRandom(new LogisticDistribution(1.41, 3.14, new Random(0L)), 10000, 1e-2);
    checkRandom(new LogisticDistribution(3.14, 1.41, new Random(0L)), 10000, 1e-2);
  }
}
