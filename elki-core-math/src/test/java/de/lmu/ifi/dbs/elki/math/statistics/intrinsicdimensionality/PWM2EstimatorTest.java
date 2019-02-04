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
package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for PWM2 estimator.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class PWM2EstimatorTest extends AbstractIntrinsicDimensionalityEstimatorTest {
  @Test
  public void testPWM2() {
    IntrinsicDimensionalityEstimator est = new ELKIBuilder<>(PWM2Estimator.class).build();
    regressionTest(est, 5, 1000, 0L, 4.88144168533192);
    regressionTest(est, 7, 10000, 0L, 6.9603914177038435);
  }

  @Test(expected = ArithmeticException.class)
  public void testFailWithZeros() {
    testZeros(PWM2Estimator.STATIC);
  }
}
