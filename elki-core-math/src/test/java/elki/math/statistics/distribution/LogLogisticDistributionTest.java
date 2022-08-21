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
 * Unit test for the Log Logistic distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogLogisticDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("loglogistic.ascii.gz");
    assertPDF(new LogLogisticDistribution(1., 0., 1.), "pdf_scipy_1_1", 1e-15);
    assertPDF(new LogLogisticDistribution(2., 0., .5), "pdf_scipy_2_05", 1e-15);
    assertPDF(new LogLogisticDistribution(.5, 0., .5), "pdf_scipy_05_05", 1e-14);
  }

  @Test
  public void testLogPDF() {
    load("loglogistic.ascii.gz");
    assertLogPDF(new LogLogisticDistribution(1., 0., 1.), "logpdf_scipy_1_1", 1e-15);
    assertLogPDF(new LogLogisticDistribution(2., 0., .5), "logpdf_scipy_2_05", 1e-15);
    assertLogPDF(new LogLogisticDistribution(.5, 0., .5), "logpdf_scipy_05_05", 1e-15);
  }

  @Test
  public void testCDF() {
    load("loglogistic.ascii.gz");
    assertCDF(new LogLogisticDistribution(1., 0., 1.), "cdf_scipy_1_1", 1e-15);
    assertCDF(new LogLogisticDistribution(2., 0., .5), "cdf_scipy_2_05", 1e-15);
    assertCDF(new LogLogisticDistribution(.5, 0., .5), "cdf_scipy_05_05", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("loglogistic.ascii.gz");
    assertQuantile(new LogLogisticDistribution(1., 0., 1.), "quant_scipy_1_1", 1e-12);
    assertQuantile(new LogLogisticDistribution(2., 0., .5), "quant_scipy_2_05", 1e-12);
    assertQuantile(new LogLogisticDistribution(.5, 0., .5), "quant_scipy_05_05", 1e-11);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("loglogistic.ascii.gz");
    Distribution dist = new ELKIBuilder<>(LogLogisticDistribution.class) //
        .with(LogLogisticDistribution.Par.SHAPE_ID, 2.) //
        .with(LogLogisticDistribution.Par.LOCATION_ID, 0.) //
        .with(LogLogisticDistribution.Par.SCALE_ID, .5).build();
    assertPDF(dist, "pdf_scipy_2_05", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new LogLogisticDistribution(0.9, 1, 0.1), new Random(0L), 10000, 1e-2);
    assertRandom(new LogLogisticDistribution(3.14, 2, 1.41), new Random(0L), 10000, 1e-2);
    assertRandom(new LogLogisticDistribution(1.41, 3, 3.14), new Random(0L), 10000, 1e-2);
  }
}
