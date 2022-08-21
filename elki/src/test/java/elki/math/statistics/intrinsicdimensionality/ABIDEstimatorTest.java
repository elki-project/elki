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
package elki.math.statistics.intrinsicdimensionality;

import org.junit.Test;

/**
 * Test the ABID estimator.
 * <p>
 * Note: we cannot move this test to the math package, because we do not want to
 * have it depend on the database and queries to avoid circular dependencies.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class ABIDEstimatorTest {
  @Test
  public void testEstimator() {
    IDEstimatorTest.regressionTest(ABIDEstimator.STATIC, 5, 1000, 200, 1L, 4.92354963);
  }
}
