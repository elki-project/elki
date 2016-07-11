package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaChoiWetteEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;

/**
 * Unit test for the Gamma distribution in ELKI.
 * 
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class GammaDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("gamma.ascii.gz");
    checkPDF(new GammaDistribution(1., 1.), "pdf_scipy_1_1", 1e-15);
    checkPDF(new GammaDistribution(2., 1.), "pdf_scipy_2_1", 1e-15);
    checkPDF(new GammaDistribution(4., 1.), "pdf_scipy_4_1", 1e-15);
    checkPDF(new GammaDistribution(4., 10), "pdf_scipy_4_10", 1e-15);
    checkPDF(new GammaDistribution(.1, 10), "pdf_scipy_01_10", 1e-15);
    checkPDF(new GammaDistribution(.1, 20), "pdf_scipy_01_20", 1e-15);
    checkPDF(new GammaDistribution(.1, 4.), "pdf_scipy_01_4", 1e-15);
    checkPDF(new GammaDistribution(.1, 1.), "pdf_scipy_01_1", 1e-15);

    checkPDF(new GammaDistribution(1., 1.), "pdf_gnur_1_1", 1e-15);
    checkPDF(new GammaDistribution(2., 1.), "pdf_gnur_2_1", 1e-16);
    checkPDF(new GammaDistribution(4., 1.), "pdf_gnur_4_1", 1e-14);
    checkPDF(new GammaDistribution(4., 10), "pdf_gnur_4_10", 1e-14);
    checkPDF(new GammaDistribution(.1, 10), "pdf_gnur_01_10", 1e-15);
    checkPDF(new GammaDistribution(.1, 20), "pdf_gnur_01_20", 1e-14);
    checkPDF(new GammaDistribution(.1, 4.), "pdf_gnur_01_4", 1e-15);
    checkPDF(new GammaDistribution(.1, 1.), "pdf_gnur_01_1", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("gamma.ascii.gz");
    checkLogPDF(new GammaDistribution(1., 1.), "logpdf_scipy_1_1", 1e-15);
    checkLogPDF(new GammaDistribution(2., 1.), "logpdf_scipy_2_1", 1e-15);
    checkLogPDF(new GammaDistribution(4., 1.), "logpdf_scipy_4_1", 1e-15);
    checkLogPDF(new GammaDistribution(4., 10), "logpdf_scipy_4_10", 1e-14);
    checkLogPDF(new GammaDistribution(.1, 10), "logpdf_scipy_01_10", 1e-15);
    checkLogPDF(new GammaDistribution(.1, 20), "logpdf_scipy_01_20", 1e-15);
    checkLogPDF(new GammaDistribution(.1, 4.), "logpdf_scipy_01_4", 1e-15);
    checkLogPDF(new GammaDistribution(.1, 1.), "logpdf_scipy_01_1", 1e-15);

    checkLogPDF(new GammaDistribution(1., 1.), "logpdf_gnur_1_1", 1e-15);
    checkLogPDF(new GammaDistribution(2., 1.), "logpdf_gnur_2_1", 1e-16);
    checkLogPDF(new GammaDistribution(4., 1.), "logpdf_gnur_4_1", 1e-14);
    checkLogPDF(new GammaDistribution(4., 10), "logpdf_gnur_4_10", 1e-14);
    checkLogPDF(new GammaDistribution(.1, 10), "logpdf_gnur_01_10", 1e-15);
    checkLogPDF(new GammaDistribution(.1, 20), "logpdf_gnur_01_20", 1e-14);
    checkLogPDF(new GammaDistribution(.1, 4.), "logpdf_gnur_01_4", 1e-15);
    checkLogPDF(new GammaDistribution(.1, 1.), "logpdf_gnur_01_1", 1e-15);
  }

  @Test
  public void testCDF() {
    load("gamma.ascii.gz");
    checkCDF(new GammaDistribution(1., 1.), "cdf_scipy_1_1", 1e-15);
    checkCDF(new GammaDistribution(2., 1.), "cdf_scipy_2_1", 1e-15);
    checkCDF(new GammaDistribution(4., 1.), "cdf_scipy_4_1", 1e-15);
    checkCDF(new GammaDistribution(4., 10), "cdf_scipy_4_10", 1e-15);
    checkCDF(new GammaDistribution(.1, 10), "cdf_scipy_01_10", 1e-15);
    checkCDF(new GammaDistribution(.1, 20), "cdf_scipy_01_20", 1e-15);
    checkCDF(new GammaDistribution(.1, 4.), "cdf_scipy_01_4", 1e-15);
    checkCDF(new GammaDistribution(.1, 1.), "cdf_scipy_01_1", 1e-15);

    checkCDF(new GammaDistribution(1., 1.), "cdf_gnur_1_1", 1e-15);
    checkCDF(new GammaDistribution(2., 1.), "cdf_gnur_2_1", 1e-15);
    checkCDF(new GammaDistribution(4., 1.), "cdf_gnur_4_1", 1e-14);
    checkCDF(new GammaDistribution(4., 10), "cdf_gnur_4_10", 1e-15);
    checkCDF(new GammaDistribution(.1, 10), "cdf_gnur_01_10", 1e-15);
    checkCDF(new GammaDistribution(.1, 20), "cdf_gnur_01_20", 1e-15);
    checkCDF(new GammaDistribution(.1, 4.), "cdf_gnur_01_4", 1e-15);
    checkCDF(new GammaDistribution(.1, 1.), "cdf_gnur_01_1", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("gamma.ascii.gz");
    checkQuantile(new GammaDistribution(1., 1.), "quant_scipy_1_1", 1e-14);
    checkQuantile(new GammaDistribution(2., 1.), "quant_scipy_2_1", 1e-13);
    checkQuantile(new GammaDistribution(4., 1.), "quant_scipy_4_1", 1e-13);
    checkQuantile(new GammaDistribution(4., 10), "quant_scipy_4_10", 1e-13);
    checkQuantile(new GammaDistribution(.1, 10), "quant_scipy_01_10", 1e-13);
    checkQuantile(new GammaDistribution(.1, 20), "quant_scipy_01_20", 1e-14);
    checkQuantile(new GammaDistribution(.1, 4.), "quant_scipy_01_4", 1e-13);
    checkQuantile(new GammaDistribution(.1, 1.), "quant_scipy_01_1", 1e-13);

    checkQuantile(new GammaDistribution(1., 1.), "quant_gnur_1_1", 1e-14);
    checkQuantile(new GammaDistribution(2., 1.), "quant_gnur_2_1", 1e-13);
    checkQuantile(new GammaDistribution(4., 1.), "quant_gnur_4_1", 1e-13);
    checkQuantile(new GammaDistribution(4., 10), "quant_gnur_4_10", 1e-13);
    checkQuantile(new GammaDistribution(.1, 10), "quant_gnur_01_10", 1e-13);
    checkQuantile(new GammaDistribution(.1, 20), "quant_gnur_01_20", 1e-14);
    checkQuantile(new GammaDistribution(.1, 4.), "quant_gnur_01_4", 1e-13);
    checkQuantile(new GammaDistribution(.1, 1.), "quant_gnur_01_1", 1e-13);
  }

  @Test
  public void testRandomAndEstimation() {
    GammaDistribution g = new GammaDistribution(1.2345, 0.12345, new Random(0));
    double[] data = new double[10000];
    for(int i = 0; i < data.length; i++) {
      data[i] = g.nextRandom();
    }
    GammaDistribution g2 = GammaChoiWetteEstimator.STATIC.estimate(data, ArrayLikeUtil.DOUBLEARRAYADAPTER);
    assertEquals("k does not match.", g.getK(), g2.getK(), 1E-2);
    assertEquals("theta does not match.", g.getTheta(), g2.getTheta(), 1E-5);
  }

  @Test
  public void extremeValues() {
    // TODO: tested values
    GammaDistribution dist = new GammaDistribution(1.1987906546993674E12, 1.1987905236089673E12);
    assertEquals(0.0, dist.cdf(Double.MIN_VALUE), 1e-50);
    // FIXME: NEITHER OF THESE VALUE IS NOT VERIFIED.
    // THIS IS SOLELY A REGRESSION TEST.
    // assertEquals(0.45430463189141745, dist.cdf(1.0), 1e-15);
    assertEquals(3.6330826914761E-4, dist.cdf(1.0), 1e-15);
  }
}
