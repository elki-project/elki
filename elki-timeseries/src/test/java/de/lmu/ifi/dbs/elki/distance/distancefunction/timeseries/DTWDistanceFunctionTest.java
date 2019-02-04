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
package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Unit test for dynamic time warping distance.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DTWDistanceFunctionTest {
  /**
   * Some test series.
   */
  final double[][] DATA = { //
  { 1., 2., 3., 4. },//
  { 1., 1., 2., 2., 3., 3., 4., 4. },//
  { 1., 1., 1., 1., 1., 2., 3., 4. },//
  { 1., 2., 3., 4., 4., 4., 4., 4. },//
  { 4., 3., 2., 1., }, //
  { 4., 4., 3., 3., 2., 2., 1., 1. }, //
  { 2.5 }, //
  };

  /**
   * Triangular matrix with deltas.
   */
  final double[][] FULLWIDTH_SCORES = { //
  { 0., 0., 0., Math.sqrt(20), Math.sqrt(28), Math.sqrt(5) }, //
  { 0., 0., Math.sqrt(28), Math.sqrt(40), Math.sqrt(10) }, //
  { 0., Math.sqrt(26), Math.sqrt(40), Math.sqrt(14) }, //
  { Math.sqrt(26), Math.sqrt(40), Math.sqrt(14) }, //
  { 0., Math.sqrt(5) }, //
  { Math.sqrt(10) }, //
  {}, //
  };

  /**
   * Triangular matrix with deltas.
   *
   * Infinity arises when lengths are too different!
   */
  final double[][] CONSTRAINED_SCORES = { //
  { INF, INF, INF, Math.sqrt(20), INF, INF }, //
  { 0., 0., INF, Math.sqrt(40), INF }, //
  { Math.sqrt(3), INF, Math.sqrt(40), INF }, //
  { INF, Math.sqrt(40), INF }, //
  { INF, INF }, //
  { INF }, //
  {}, //
  };

  /**
   * Shorthand.
   */
  protected static final double INF = Double.POSITIVE_INFINITY;

  @Test
  public void testDynamicTimeWarping() {
    DoubleVector[] vecs = new DoubleVector[DATA.length];
    for(int i = 0; i < DATA.length; i++) {
      vecs[i] = DoubleVector.wrap(DATA[i]);
    }
    DTWDistanceFunction f = new DTWDistanceFunction();
    for(int i = 0; i < vecs.length; i++) {
      for(int j = 0; j < vecs.length; j++) {
        double dist = f.distance(vecs[i], vecs[j]);
        double exp = (i == j) ? 0. : (i < j) ? FULLWIDTH_SCORES[i][j - i - 1] : FULLWIDTH_SCORES[j][i - j - 1];
        assertEquals("Distance does not agree: " + vecs[i] + " <-> " + vecs[j], exp, dist, 1e-15);
      }
    }
  }

  @Test
  public void testConstrainedDynamicTimeWarping() {
    DoubleVector[] vecs = new DoubleVector[DATA.length];
    for(int i = 0; i < DATA.length; i++) {
      vecs[i] = DoubleVector.wrap(DATA[i]);
    }
    // 4,4 -> 2; 4,8 -> 3; 8,8 -> 3
    DTWDistanceFunction f = new DTWDistanceFunction(.33);
    for(int i = 0; i < vecs.length; i++) {
      for(int j = 0; j < vecs.length; j++) {
        double dist = f.distance(vecs[i], vecs[j]);
        if(j - i > CONSTRAINED_SCORES[i].length) {
          System.err.println("Missing distance is: " + dist);
        }
        double exp = (i == j) ? 0. : (i < j) ? CONSTRAINED_SCORES[i][j - i - 1] : CONSTRAINED_SCORES[j][i - j - 1];
        assertEquals("Distance does not agree: " + vecs[i] + " <-> " + vecs[j], exp, dist, 1e-15);
      }
    }
  }
}
