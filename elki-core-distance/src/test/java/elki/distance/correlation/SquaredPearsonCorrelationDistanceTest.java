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
package elki.distance.correlation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.distance.AbstractDistanceTest;

/**
 * Unit test for squared Pearson correlation distance.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SquaredPearsonCorrelationDistanceTest extends AbstractDistanceTest {
  /** Test set */
  final static DoubleVector[] TESTS = PearsonCorrelationDistanceTest.TESTS;

  /** Note, these are not yet adjusted */
  final static double[][] SCORES = PearsonCorrelationDistanceTest.SCORES;

  @Test
  public void testSquaredPearson() {
    SquaredPearsonCorrelationDistance f = SquaredPearsonCorrelationDistance.STATIC;
    basicChecks(f);
    for(int i = 0; i < TESTS.length; i++) {
      for(int j = 0; j < TESTS.length; j++) {
        final double dist = f.distance(TESTS[i], TESTS[j]);
        final double r = 1. - SCORES[i][j];
        assertEquals("Distance does not agree: " + TESTS[i] + " <-> " + TESTS[j], 1. - r * r, dist, 1e-15);
      }
    }
  }
}
