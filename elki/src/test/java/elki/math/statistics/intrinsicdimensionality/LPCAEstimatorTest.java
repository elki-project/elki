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

import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.math.linearalgebra.pca.filter.RelativeEigenPairFilter;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for LPCA ID estimator.
 * 
 * @author Erik Thordsen
 * @since 0.7.6
 */
public class LPCAEstimatorTest {
  @Test
  public void testLPCA() {
    /* In a uniform d-ball, every eigenvalue should account for 1/d of the variance. */
    testLPCAFilter(new PercentageEigenPairFilter(.95), 5, 1000, 200, 1L, 5.0);
    testLPCAFilter(new PercentageEigenPairFilter(.75), 5, 1000, 200, 1L, 4.0);
    testLPCAFilter(new PercentageEigenPairFilter(.55), 5, 1000, 200, 1L, 3.0);
    testLPCAFilter(new PercentageEigenPairFilter(.35), 5, 1000, 200, 1L, 2.0);
    testLPCAFilter(new PercentageEigenPairFilter(.15), 5, 1000, 200, 1L, 1.0);

    /* Some test cases for the relative filter. This is mostly gut feeling, might need some rework. */
    testLPCAFilter(new RelativeEigenPairFilter(1.1), 5, 1000, 200, 1L, 2.0);
    testLPCAFilter(new RelativeEigenPairFilter(1.), 5, 1000, 200, 1L, 4.0);
  }

  /* Wrapper to test with specified EigenPairFilter. */
  private void testLPCAFilter(EigenPairFilter filter, int dim, int size, int k, long seed, double edim) {
    LPCAEstimator e = new ELKIBuilder<>(LPCAEstimator.class) //
        .with(LPCAEstimator.Par.FILTER_ID, filter).build();
    IDEstimatorTest.regressionTest(e, dim, size, k, seed, edim);
  }
}
