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
package elki.distance.set;

import org.junit.Test;

import elki.utilities.ELKIBuilder;

/**
 * Test for Hamming Distance.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class HammingDistanceTest extends AbstractSetDistanceTest {
  /**
   * Expected distance values
   */
  private static final double[] SCORES = { 10, // # a != b = 10
      5, // # c != d = 5
      5, // # c != e = 5
      4, // # a != c = 4
      3, // # b != d = 3
      0 // # e != e = 0
  };

  @Test
  public void testHammingDistance() {
    HammingDistance dist = new ELKIBuilder<>(HammingDistance.class).build();

    basicChecks(dist);
    assertVaryingLengthBasic(dist, new double[] { 1, 0, 1, 1, 2, 1 }, 0);

    assertBitVectorDistances(dist, SCORES, 0);
    assertNumberVectorDistances(dist, SCORES, 0);
    assertFeatureVectorDistances(dist, SCORES, 0);
    assertIntegerVectorVarLen(dist, 8, 0);
  }
}
