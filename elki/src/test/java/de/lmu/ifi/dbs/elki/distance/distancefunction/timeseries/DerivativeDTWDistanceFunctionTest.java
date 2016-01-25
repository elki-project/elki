package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Unit test for dynamic time warping distance.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DerivativeDTWDistanceFunctionTest implements JUnit4Test {
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
  { 1., 2., 3., 4., 5., 6., 7., 8. }, // Distance 0 to first!
  };

  /**
   * Triangular matrix with deltas.
   */
  final double[][] FULLWIDTH_SCORES = { //
  { Math.sqrt(2.5), Math.sqrt(4.563), Math.sqrt(4.06), Math.sqrt(16), Math.sqrt(18.5), 0. }, //
  { Math.sqrt(.937), Math.sqrt(2.687), Math.sqrt(18.5), Math.sqrt(9.25), Math.sqrt(2.5) }, //
  { Math.sqrt(6.625), Math.sqrt(17.556), Math.sqrt(9.25), Math.sqrt(4.563) }, //
  { Math.sqrt(19.06), Math.sqrt(8.06), Math.sqrt(4.062) }, //
  { Math.sqrt(2.5), Math.sqrt(32) }, //
  { Math.sqrt(18.5) }, //
  {}, //
  };

  @Test
  public void testDerivativeDynamicTimeWarping() {
    DoubleVector[] vecs = new DoubleVector[DATA.length];
    for(int i = 0; i < DATA.length; i++) {
      vecs[i] = DoubleVector.wrap(DATA[i]);
    }
    DerivativeDTWDistanceFunction f = new DerivativeDTWDistanceFunction();
    for(int i = 0; i < vecs.length; i++) {
      for(int j = 0; j < vecs.length; j++) {
        double dist = f.distance(vecs[i], vecs[j]);
        if(j - i > FULLWIDTH_SCORES[i].length) {
          System.err.println("Missing distance is: " + dist);
        }
        double exp = (i == j) ? 0. : (i < j) ? FULLWIDTH_SCORES[i][j - i - 1] : FULLWIDTH_SCORES[j][i - j - 1];
        assertEquals("Distance does not agree: " + vecs[i] + " <-> " + vecs[j], exp, dist, 1e-3);
      }
    }
  }
}
