package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/**
 * Unit test for triangular distance.
 * 
 * @author Erich Schubert
 */
public class TriangularDistanceFunctionTest {
  @Test
  public void testTriganular() {
    DoubleVector v0 = DoubleVector.wrap(new double[] { 0.8, 0.1, 0.1 });
    DoubleVector v1 = DoubleVector.wrap(new double[] { 0.1, 0.8, 0.1 });
    DoubleVector v2 = DoubleVector.wrap(new double[] { 0.1, 0.1, 0.8 });
    DoubleVector v3 = DoubleVector.wrap(new double[] { 1. / 3, 1. / 3, 1. / 3 });
    DoubleVector v4 = DoubleVector.wrap(new double[] { 0.6, 0.2, 0.2 });

    DoubleVector[] vecs = { v0, v1, v2, v3, v4 };

    // Manual computation of correct distances:
    double d10 = .49 / .9 + .49 / .9 + 0.;
    double d30 = (.8 - 1. / 3) * (.8 - 1. / 3) / (.8 + 1. / 3) //
        + 2 * (.1 - 1. / 3) * (.1 - 1. / 3) / (.1 + 1. / 3);
    double d40 = .04 / 1.4 + .01 / .3 + .01 / .3;
    double d41 = .25 / 0.7 + .36 / 1. + .01 / .3;
    double d43 = (.6 - 1. / 3) * (.6 - 1. / 3) / (.6 + 1. / 3) //
        + 2 * (.2 - 1. / 3) * (.2 - 1. / 3) / (.2 + 1. / 3);
    double[][] distances = { //
        { 0., d10, d10, d30, d40 }, //
        { d10, 0., d10, d30, d41 }, //
        { d10, d10, 0., d30, d41 }, //
        { d30, d30, d30, 0., d43 }, //
        { d40, d41, d41, d43, 0. }, //
    };

    PrimitiveDistanceFunction<NumberVector> df = TriangularDistanceFunction.STATIC;
    for(int i = 0; i < vecs.length; i++) {
      for(int j = 0; j < vecs.length; j++) {
        assertEquals("Distance " + i + "," + j + " incorrect.", Math.sqrt(distances[i][j]), df.distance(vecs[i], vecs[j]), 1e-15);
      }
    }
  }
}
