/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the Normal distribution in ELKI.
 * 
 * The reference values were computed using GNU R.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class ExponentiallyModifiedGaussianDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("emg.ascii.gz");
    checkPDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), "pdf_gnur_1_3_05", 1e-15);
  }

  @Test
  public void testCDF() {
    load("emg.ascii.gz");
    checkCDF(new ExponentiallyModifiedGaussianDistribution(1., 3., .5), "cdf_gnur_1_3_05", 1e-14);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("emg.ascii.gz");
    ListParameterization params = new ListParameterization();
    params.addParameter(ExponentiallyModifiedGaussianDistribution.Parameterizer.LOCATION_ID, 1.);
    params.addParameter(ExponentiallyModifiedGaussianDistribution.Parameterizer.SCALE_ID, 3);
    params.addParameter(ExponentiallyModifiedGaussianDistribution.Parameterizer.RATE_ID, .5);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(ExponentiallyModifiedGaussianDistribution.class, params);
    checkPDF(dist, "pdf_gnur_1_3_05", 1e-15);
  }
}
