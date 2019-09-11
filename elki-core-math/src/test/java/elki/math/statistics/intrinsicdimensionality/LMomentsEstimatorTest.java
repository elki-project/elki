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
package elki.math.statistics.intrinsicdimensionality;

import org.junit.Test;

import elki.utilities.ELKIBuilder;

/**
 * Unit test for Hill estimator.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LMomentsEstimatorTest extends AbstractIntrinsicDimensionalityEstimatorTest {
  @Test
  public void testLM() {
    IntrinsicDimensionalityEstimator est = new ELKIBuilder<>(LMomentsEstimator.class).build();
    regressionTest(est, 5, 1000, 0L, 4.8417151683258215);
    regressionTest(est, 7, 10000, 0L, 6.932108185670673);
  }

  @Test(expected = ArithmeticException.class)
  public void testFailWithZeros() {
    testZeros(LMomentsEstimator.STATIC);
  }
}
