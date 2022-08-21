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
package elki.math.statistics.dependence;

import org.junit.Test;

import elki.math.statistics.tests.KolmogorovSmirnovTest;
import elki.math.statistics.tests.WelchTTest;
import elki.utilities.ELKIBuilder;

/**
 * Ensure basic integrity.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HiCSDependenceTest extends DependenceTest {
  @Test
  public void testKS() {
    Dependence cor = new ELKIBuilder<>(HiCSDependence.class) //
        .with(HiCSDependence.Par.SEED_ID, 4) //
        .with(HiCSDependence.Par.TEST_ID, KolmogorovSmirnovTest.STATIC) //
        .build();
    // Linear correlations do affect marginal distributions!
    assertPerfectLinear(cor, 1000, 0.5, 0.12, 0.08);
    // Note that for HiCS, even independent uniform is related,
    // because the marginal distribution does not change.
    assertUniform(cor, 1000, 0.5, 0.01, 0.5, 0.08);
    // Gaussian marginals are also related
    assertGaussians(cor, 1000, 0.62, 0.08);
  }

  @Test
  public void testWelch() {
    Dependence cor = new ELKIBuilder<>(HiCSDependence.class) //
        .with(HiCSDependence.Par.SEED_ID, 0) //
        .with(HiCSDependence.Par.TEST_ID, WelchTTest.STATIC) //
        .build();
    // This test mostly shows that Welch-T is too aggressive for this to work:
    // Even i.i.d. distributed data frequently pass as different.
    assertPerfectLinear(cor, 1000, 0.07, 0., 0.02);
    assertUniform(cor, 1000, 0.01, 0.05, 0., 0.08);
    assertGaussians(cor, 1000, 0.02, 0.08);
  }
}
