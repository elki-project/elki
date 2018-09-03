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

import static net.jafama.FastMath.sqrt;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialPrimitiveDistanceFunctionTest;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for Chi distance.
 *
 * @author Erich Schubert
 */
public class ChiDistanceFunctionTest extends AbstractSpatialPrimitiveDistanceFunctionTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    ChiDistanceFunction df = new ELKIBuilder<>(ChiDistanceFunction.class).build();
    nonnegativeSpatialConsistency(df);
  }

  @Test
  public void testChiSquaredDistance() {
    double[][] vecs = TOY_VECTORS;
    double[][] distances = ChiSquaredDistanceFunctionTest.TOY_DISTANCES;
    ChiDistanceFunction df = new ELKIBuilder<>(ChiDistanceFunction.class).build();
    ChiSquaredDistanceFunction df2 = new ELKIBuilder<>(ChiSquaredDistanceFunction.class).build();
    for(int i = 0; i < vecs.length; i++) {
      DoubleVector vi = DoubleVector.wrap(vecs[i]);
      HyperBoundingBox mbri = new HyperBoundingBox(vecs[i], vecs[i]);
      for(int j = 0; j < vecs.length; j++) {
        DoubleVector vj = DoubleVector.wrap(vecs[j]);
        assertEquals("Distance " + i + "," + j, sqrt(distances[i][j]), df.distance(vi, vj), 1e-15);
        assertEquals("Distance " + i + "," + j, distances[i][j], df2.distance(vi, vj), 1e-15);
        compareDistances(vj, vi, mbri, df);
      }
    }
  }
}
