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
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialPrimitiveDistanceFunctionTest;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for triangular discrimination distance.
 *
 * @author Erich Schubert
 */
public class TriangularDiscriminationDistanceFunctionTest extends AbstractSpatialPrimitiveDistanceFunctionTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    TriangularDiscriminationDistanceFunction df = new ELKIBuilder<>(TriangularDiscriminationDistanceFunction.class).build();
    nonnegativeSpatialConsistency(df);
  }

  @Test
  public void testTriganular() {
    double[][] vecs = TOY_VECTORS;

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

    TriangularDiscriminationDistanceFunction df = new ELKIBuilder<>(TriangularDiscriminationDistanceFunction.class).build();
    for(int i = 0; i < vecs.length; i++) {
      DoubleVector vi = DoubleVector.wrap(vecs[i]);
      HyperBoundingBox mbri = new HyperBoundingBox(vecs[i], vecs[i]);
      for(int j = 0; j < vecs.length; j++) {
        DoubleVector vj = DoubleVector.wrap(vecs[j]);
        assertEquals("Distance " + i + "," + j, distances[i][j], df.distance(vi, vj), 1e-15);
        compareDistances(vj, vi, mbri, df);
      }
    }
  }
}
