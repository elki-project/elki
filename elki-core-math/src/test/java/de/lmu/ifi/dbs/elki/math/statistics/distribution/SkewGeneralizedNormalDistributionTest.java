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
 * Unit test for the skew generalized Normal distribution in ELKI.
 * 
 * The reference values were computed using GNU R.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SkewGeneralizedNormalDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("norm.ascii.gz");
    checkPDF(new SkewGeneralizedNormalDistribution(1., 3., 0.), "pdf_gnur_1_3", 1e-15);
    load("skewnorm.ascii.gz");
    checkPDF(new SkewGeneralizedNormalDistribution(1., 2., 3.), "pdf_gnur_1_2_3", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("norm.ascii.gz");
    checkLogPDF(new SkewGeneralizedNormalDistribution(1., 3., 0.), "logpdf_gnur_1_3", 1e-15);
    // TODO: add logspace test of skewed case
  }

  @Test
  public void testCDF() {
    load("norm.ascii.gz");
    checkCDF(new SkewGeneralizedNormalDistribution(1., 3., 0.), "cdf_gnur_1_3", 1e-15);
    load("skewnorm.ascii.gz");
    checkCDF(new SkewGeneralizedNormalDistribution(1., 2., 3), "cdf_gnur_1_2_3", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("norm.ascii.gz");
    checkQuantile(new SkewGeneralizedNormalDistribution(1., 3., 0.), "quant_gnur_1_3", 1e-15);
    load("skewnorm.ascii.gz");
    checkQuantile(new SkewGeneralizedNormalDistribution(1., 2., 3.), "quant_gnur_1_2_3", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("skewnorm.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(SkewGeneralizedNormalDistribution.Parameterizer.LOCATION_ID, 1.);
    params.addParameter(SkewGeneralizedNormalDistribution.Parameterizer.SCALE_ID, 2.);
    params.addParameter(SkewGeneralizedNormalDistribution.Parameterizer.SKEW_ID, 3.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(SkewGeneralizedNormalDistribution.class, params);
    checkPDF(dist, "pdf_gnur_1_2_3", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new SkewGeneralizedNormalDistribution(0.1, 0.9, 1, new Random(0L)), 10000, 1e-2);
    checkRandom(new SkewGeneralizedNormalDistribution(1.41, 3.14, 2, new Random(0L)), 10000, 1e-2);
    checkRandom(new SkewGeneralizedNormalDistribution(3.14, 1.41, 3, new Random(0L)), 10000, 1e-2);
  }
}
