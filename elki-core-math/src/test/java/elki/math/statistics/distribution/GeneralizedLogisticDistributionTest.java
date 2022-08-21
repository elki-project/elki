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
 * Unit test for the Generalized Logistic distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class GeneralizedLogisticDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("logistic.ascii.gz");
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., 1.), "pdf_scipy_05", 1e-15);
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., 1.), "pdf_gnur_05", 1e-15);
    load("glogistic.ascii.gz");
    assertPDF(new GeneralizedLogisticDistribution(1., 1., 1.), "pdf_scipy_1_1", 1e-15);
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., 2.), "pdf_scipy_2_05", 1e-15);
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., .5), "pdf_scipy_05_05", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("logistic.ascii.gz");
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., 1.), "logpdf_scipy_05", 1e-15);
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., 1.), "logpdf_gnur_05", 1e-15);
    load("glogistic.ascii.gz");
    assertPDF(new GeneralizedLogisticDistribution(1., 1., 1.), "logpdf_scipy_1_1", 1e-15);
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., 2.), "logpdf_scipy_2_05", 1e-15);
    assertPDF(new GeneralizedLogisticDistribution(.5, 1., .5), "logpdf_scipy_05_05", 1e-15);
  }

  @Test
  public void testCDF() {
    load("logistic.ascii.gz");
    assertCDF(new GeneralizedLogisticDistribution(.5, 1., 1.), "cdf_scipy_05", 1e-15);
    assertCDF(new GeneralizedLogisticDistribution(.5, 1., 1.), "cdf_gnur_05", 1e-15);
    load("glogistic.ascii.gz");
    assertCDF(new GeneralizedLogisticDistribution(1., 1., 1.), "cdf_scipy_1_1", 1e-15);
    assertCDF(new GeneralizedLogisticDistribution(.5, 1., 2.), "cdf_scipy_2_05", 1e-15);
    assertCDF(new GeneralizedLogisticDistribution(.5, 1., .5), "cdf_scipy_05_05", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("logistic.ascii.gz");
    assertQuantile(new GeneralizedLogisticDistribution(.5, 1., 1.), "quant_scipy_05", 1e-13);
    assertQuantile(new GeneralizedLogisticDistribution(.5, 1., 1.), "quant_gnur_05", 1e-13);
    load("glogistic.ascii.gz");
    assertQuantile(new GeneralizedLogisticDistribution(1., 1., 1.), "quant_scipy_1_1", 1e-13);
    assertQuantile(new GeneralizedLogisticDistribution(.5, 1., 2.), "quant_scipy_2_05", 1e-14);
    assertQuantile(new GeneralizedLogisticDistribution(.5, 1., .5), "quant_scipy_05_05", 1e-13);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("glogistic.ascii.gz");
    Distribution dist = new ELKIBuilder<>(GeneralizedLogisticDistribution.class) //
        .with(GeneralizedLogisticDistribution.Par.LOCATION_ID, .5) //
        .with(GeneralizedLogisticDistribution.Par.SCALE_ID, 1) //
        .with(GeneralizedLogisticDistribution.Par.SHAPE_ID, 2.).build();
    assertPDF(dist, "pdf_scipy_2_05", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new GeneralizedLogisticDistribution(0.1, 0.9, 1), new Random(0L), 10000, 1e-2);
    assertRandom(new GeneralizedLogisticDistribution(1.41, 3.14, 2), new Random(0L), 10000, 1e-2);
    assertRandom(new GeneralizedLogisticDistribution(3.14, 1.41, 3), new Random(0L), 10000, 1e-2);
  }
}
