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
package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunctionTest;

/**
 * Unit test for uncentered Pearson correlation distance.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class UncenteredCorrelationDistanceFunctionTest extends AbstractDistanceFunctionTest {
  /** Inherited test data */
  final static DoubleVector[] TESTS = PearsonCorrelationDistanceFunctionTest.TESTS;

  /** Dissimilarities */
  static final double C1 = 1.3636363636363638, C2 = 0.23636363636363633,
      C3 = 0.6363636363636364;

  /**
   * The associated scores.
   */
  static final double[][] SCORES = { //
      { 0., 0., 2., C1, 1., C2 }, //
      { 0., 0., 2., C1, 1., C2 }, //
      { 2., 2., 0., C3, 1., 2. - C2 }, //
      { C1, C1, C3, 0., 1., 1.6 }, //
      { 1., 1., 1., 1., 0., 1. }, //
      { C2, C2, 2. - C2, 1.6, 1., 0. },//
  };

  @Test
  public void testUncenteredCorrelation() {
    UncenteredCorrelationDistanceFunction f = UncenteredCorrelationDistanceFunction.STATIC;
    basicChecks(f);
    for(int i = 0; i < TESTS.length; i++) {
      for(int j = 0; j < TESTS.length; j++) {
        final double dist = f.distance(TESTS[i], TESTS[j]);
        assertEquals("Distance does not agree: " + TESTS[i] + " <-> " + TESTS[j], SCORES[i][j], dist, 1e-15);
      }
    }
  }
}
