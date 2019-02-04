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
 * Unit test for the Generalized Pareto Distribution (GPD) in ELKI.
 * 
 * The reference values were computed using SciPy.
 * 
 * GNU R has implementations in lmomco, but these do not do bounds checks, and
 * return invalid cdf and pdf outside; they also appear to have rather different
 * results than both SciPy and our implementation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class GeneralizedParetoDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("gpd.ascii.gz");
    checkPDF(new GeneralizedParetoDistribution(.1, .5, .1), "pdf_scipy_01_05_01", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("gpd.ascii.gz");
    checkLogPDF(new GeneralizedParetoDistribution(.1, .5, .1), "logpdf_scipy_01_05_01", 1e-15);
  }

  @Test
  public void testCDF() {
    load("gpd.ascii.gz");
    checkCDF(new GeneralizedParetoDistribution(.1, .5, .1), "cdf_scipy_01_05_01", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("gpd.ascii.gz");
    checkQuantile(new GeneralizedParetoDistribution(.1, .5, .1), "quant_scipy_01_05_01", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("gpd.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(GeneralizedParetoDistribution.Parameterizer.LOCATION_ID, .1);
    params.addParameter(GeneralizedParetoDistribution.Parameterizer.SCALE_ID, .5);
    params.addParameter(GeneralizedParetoDistribution.Parameterizer.SHAPE_ID, .1);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(GeneralizedParetoDistribution.class, params);
    checkPDF(dist, "pdf_scipy_01_05_01", 1e-15);
  }

  @Test
  public void testRandom() {
    checkRandom(new GeneralizedParetoDistribution(0.1, 0.9, 1, new Random(0L)), 10000, 1e-2);
    checkRandom(new GeneralizedParetoDistribution(1.41, 3.14, 2, new Random(0L)), 10000, 1e-2);
    checkRandom(new GeneralizedParetoDistribution(3.14, 1.41, 3, new Random(0L)), 10000, 1e-2);
  }
}
