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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunctionTest;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for Jensen-Shannon distance.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class JensenShannonDivergenceDistanceFunctionTest extends AbstractDistanceFunctionTest {
  @Test
  public void testBasic() {
    // Also test the builder - we could have just used .STATIC
    JensenShannonDivergenceDistanceFunction df = new ELKIBuilder<>(JensenShannonDivergenceDistanceFunction.class).build();
    basicChecks(df);
    nonnegativeSpatialConsistency(df);
  }

  @Test
  public void testJensenShannonDivergenceDistance() {
    double[][] vecs = TOY_VECTORS;
    double[][] distances = JeffreyDivergenceDistanceFunctionTest.TOY_DISTANCES;
    JensenShannonDivergenceDistanceFunction df = new ELKIBuilder<>(JensenShannonDivergenceDistanceFunction.class).build();
    for(int i = 0; i < vecs.length; i++) {
      DoubleVector vi = DoubleVector.wrap(vecs[i]);
      HyperBoundingBox mbri = new HyperBoundingBox(vecs[i], vecs[i]);
      for(int j = 0; j < vecs.length; j++) {
        DoubleVector vj = DoubleVector.wrap(vecs[j]);
        assertEquals("Distance " + i + "," + j, 0.5 * distances[i][j], df.distance(vi, vj), 1e-15);
        compareDistances(vj, vi, mbri, df);
      }
    }
  }
}
