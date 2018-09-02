/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Unit test for Chi<sup>2</sup> distance.
 *
 * @author Erich Schubert
 */
public class ChiSquaredDistanceFunctionTest {
  @Test
  public void testChiSquaredDistance() {
    DoubleVector v0 = DoubleVector.wrap(new double[] { 0.8, 0.1, 0.1 });
    DoubleVector v1 = DoubleVector.wrap(new double[] { 0.1, 0.8, 0.1 });
    DoubleVector v2 = DoubleVector.wrap(new double[] { 0.1, 0.1, 0.8 });
    DoubleVector v3 = DoubleVector.wrap(new double[] { 1. / 3, 1. / 3, 1. / 3 });
    DoubleVector v4 = DoubleVector.wrap(new double[] { 0.6, 0.2, 0.2 });

    DoubleVector[] vecs = { v0, v1, v2, v3, v4 };

    // Manual computation of correct distances:
    double d01 = 2 * (49. / 90 * 2);
    double d03 = 2 * (49. / 255 + 49. / 390 * 2);
    double d04 = 2 * (4. / 140 + 1. / 30 * 2);
    double d14 = 2 * (25. / 70 + .36 + 1. / 30);
    double d34 = 2 * (8. / 105 + 1. / 30 * 2);
    double[][] distances = { //
        { 0., d01, d01, d03, d04 }, //
        { d01, 0., d01, d03, d14 }, //
        { d01, d01, 0., d03, d14 }, //
        { d03, d03, d03, 0., d34 }, //
        { d04, d14, d14, d34, 0. }, //
    };
    ChiSquaredDistanceFunction df = ChiSquaredDistanceFunction.STATIC;
    for(int i = 0; i < vecs.length; i++) {
      for(int j = 0; j < vecs.length; j++) {
        assertEquals("Distance " + i + "," + j + " incorrect.", distances[i][j], df.distance(vecs[i], vecs[j]), 1e-15);
      }
    }
  }
}
